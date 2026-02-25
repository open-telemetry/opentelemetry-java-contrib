/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class Storage<T> implements Closeable {
  private static final int MAX_ATTEMPTS = 3;
  private final Logger logger = Logger.getLogger(Storage.class.getName());
  private final FolderManager folderManager;
  private volatile Predicate<FolderManager.CacheFile> fileExclusion = file -> false;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final AtomicBoolean activeReadResultAvailable = new AtomicBoolean(false);
  private final AtomicReference<WritableFile> writableFileRef = new AtomicReference<>();
  private final AtomicReference<ReadableFile> readableFileRef = new AtomicReference<>();

  public Storage(FolderManager folderManager) {
    this.folderManager = folderManager;
  }

  /**
   * Attempts to write an item into a writable file.
   *
   * @param marshaler - The data that would be appended to the file.
   * @throws IOException If an unexpected error happens.
   */
  public boolean write(SignalSerializer<T> marshaler) throws IOException {
    return write(marshaler, 1);
  }

  private boolean write(SignalSerializer<T> marshaler, int attemptNumber) throws IOException {
    if (isClosed.get()) {
      logger.fine("Refusing to write to storage after being closed.");
      return false;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log(WARNING, "Max number of attempts to write buffered data exceeded.");
      return false;
    }
    WritableFile writableFile = writableFileRef.get();
    if (writableFile == null) {
      writableFile = folderManager.createWritableFile();
      writableFileRef.set(writableFile);
      logger.finer("Created new writableFile: " + writableFile);
    }
    WritableResult result = writableFile.append(marshaler);
    if (result != WritableResult.SUCCEEDED) {
      // Retry with new file
      writableFileRef.set(null);
      return write(marshaler, ++attemptNumber);
    }
    return true;
  }

  public void flush() throws IOException {
    WritableFile writableFile = writableFileRef.get();
    if (writableFile != null) {
      writableFile.flush();
    } else {
      logger.info("No writable file to flush.");
    }
  }

  /**
   * Attempts to read an item from a ready-to-read file.
   *
   * @throws IOException If an unexpected error happens.
   */
  @Nullable
  public ReadableResult<T> readNext(SignalDeserializer<T> deserializer) throws IOException {
    if (activeReadResultAvailable.get()) {
      throw new IllegalStateException(
          "You must close any previous ReadableResult before requesting a new one");
    }
    return doReadNext(deserializer, 1);
  }

  @Nullable
  private ReadableResult<T> doReadNext(SignalDeserializer<T> deserializer, int attemptNumber)
      throws IOException {
    if (isClosed.get()) {
      logger.fine("Refusing to read from storage after being closed.");
      return null;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log(WARNING, "Maximum number of attempts to read buffered data exceeded.");
      return null;
    }
    ReadableFile readableFile = readableFileRef.get();
    if (readableFile != null && readableFile.isClosed()) {
      // The file was deleted from the iterator
      readableFileRef.set(null);
      readableFile = null;
    }
    if (readableFile == null) {
      logger.finer("Obtaining a new readableFile from the folderManager.");
      readableFile = folderManager.getReadableFile(Objects.requireNonNull(fileExclusion));
      readableFileRef.set(readableFile);
      if (readableFile == null) {
        logger.fine("Unable to get or create readable file.");
        return null;
      }
    }

    logger.finer("Attempting to read data from " + readableFile);
    try {
      byte[] result = readableFile.readNext();
      if (result != null) {
        try {
          List<T> items = deserializer.deserialize(result);
          activeReadResultAvailable.set(true);
          return new FileReadResult(items, readableFile);
        } catch (DeserializationException e) {
          // Data corrupted, clear file.
          readableFile.clear();
        }
      }
    } catch (IOException e) {
      // Proto data corrupted, clear file.
      readableFile.clear();
    }

    // Search for newer files than the current one.
    long currentFileCreatedTime = readableFile.getCreatedTimeMillis();
    fileExclusion = file -> file.getCreatedTimeMillis() <= currentFileCreatedTime;
    readableFile.close();
    readableFileRef.set(null);
    return doReadNext(deserializer, ++attemptNumber);
  }

  public void clear() throws IOException {
    folderManager.clear();
  }

  public boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public void close() throws IOException {
    logger.fine("Closing disk buffering storage.");
    if (isClosed.compareAndSet(false, true)) {
      folderManager.close();
      writableFileRef.set(null);
      readableFileRef.set(null);
    }
  }

  class FileReadResult implements ReadableResult<T> {
    private final Collection<T> content;
    private final AtomicBoolean itemDeleted = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<ReadableFile> readableFile = new AtomicReference<>();

    FileReadResult(Collection<T> content, ReadableFile readableFile) {
      this.content = content;
      this.readableFile.set(readableFile);
    }

    @Override
    public Collection<T> getContent() {
      return content;
    }

    @Override
    public void delete() throws IOException {
      if (closed.get()) {
        return;
      }
      if (itemDeleted.compareAndSet(false, true)) {
        try {
          Objects.requireNonNull(readableFile.get()).removeTopItem();
        } catch (IOException e) {
          itemDeleted.set(false);
          throw e;
        }
      }
    }

    @Override
    public void close() throws IOException {
      if (closed.compareAndSet(false, true)) {
        activeReadResultAvailable.set(false);
        readableFile.set(null);
      }
    }
  }
}
