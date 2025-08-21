/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class Storage<T> implements Closeable {
  private static final int MAX_ATTEMPTS = 3;
  private final DebugLogger logger;
  private final FolderManager folderManager;
  private final boolean debugEnabled;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final AtomicBoolean activeReadResultAvailable = new AtomicBoolean(false);
  private final AtomicReference<WritableFile> writableFileRef = new AtomicReference<>();
  private final AtomicReference<ReadableFile> readableFileRef = new AtomicReference<>();

  public Storage(FolderManager folderManager, boolean debugEnabled) {
    this.folderManager = folderManager;
    this.logger =
        DebugLogger.wrap(Logger.getLogger(FromDiskExporterImpl.class.getName()), debugEnabled);
    this.debugEnabled = debugEnabled;
  }

  public static StorageBuilder builder(SignalTypes types) {
    return new StorageBuilder(types);
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
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
      logger.log("Refusing to write to storage after being closed.");
      return false;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log("Max number of attempts to write buffered data exceeded.", WARNING);
      return false;
    }
    WritableFile writableFile = writableFileRef.get();
    if (writableFile == null) {
      writableFile = folderManager.createWritableFile();
      writableFileRef.set(writableFile);
      logger.log("Created new writableFile: " + writableFile);
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
      logger.log("No writable file to flush.");
    }
  }

  /**
   * Attempts to read an item from a ready-to-read file.
   *
   * @throws IOException If an unexpected error happens.
   */
  public ReadableResult<T> readNext(SignalDeserializer<T> deserializer) throws IOException {
    if (activeReadResultAvailable.get()) {
      throw new IllegalStateException(
          "You must close any previous ReadableResult before requesting a new one");
    }
    return doReadAndProcess(deserializer, 1);
  }

  private ReadableResult<T> doReadAndProcess(SignalDeserializer<T> deserializer, int attemptNumber)
      throws IOException {
    if (isClosed.get()) {
      logger.log("Refusing to read from storage after being closed.");
      return null;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log("Maximum number of attempts to read and process buffered data exceeded.", WARNING);
      return null;
    }
    ReadableFile readableFile = readableFileRef.get();
    if (readableFile == null) {
      logger.log("Obtaining a new readableFile from the folderManager.");
      readableFile = folderManager.getReadableFile();
      readableFileRef.set(readableFile);
      if (readableFile == null) {
        logger.log("Unable to get or create readable file.");
        return null;
      }
    }

    logger.log("Attempting to read data from " + readableFile);
    byte[] result = readableFile.readNext();
    if (result != null) {
      try {
        Collection<T> items = deserializer.deserialize(result);
        return new FileReadResult(items, readableFile);
      } catch (DeserializationException e) {
        // Data corrupted, clear file.
        readableFile.clear();
      }
    }

    // Retry with new file
    readableFileRef.set(null);
    return doReadAndProcess(deserializer, ++attemptNumber);
  }

  public void clear() throws IOException {
    folderManager.clear();
  }

  public boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public void close() throws IOException {
    logger.log("Closing disk buffering storage.");
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
          readableFile.get().removeTopItem();
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
