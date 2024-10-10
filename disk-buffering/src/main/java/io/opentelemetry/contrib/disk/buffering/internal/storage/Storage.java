/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class Storage implements Closeable {
  private static final int MAX_ATTEMPTS = 3;
  private final DebugLogger logger;

  private final FolderManager folderManager;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  @Nullable private WritableFile writableFile;
  @Nullable private ReadableFile readableFile;

  public Storage(FolderManager folderManager, boolean debugEnabled) {
    this.folderManager = folderManager;
    this.logger =
        DebugLogger.wrap(Logger.getLogger(FromDiskExporterImpl.class.getName()), debugEnabled);
  }

  public static StorageBuilder builder() {
    return new StorageBuilder();
  }

  /**
   * Attempts to write an item into a writable file.
   *
   * @param item - The data that would be appended to the file.
   * @throws IOException If an unexpected error happens.
   */
  public boolean write(byte[] item) throws IOException {
    return write(item, 1);
  }

  private boolean write(byte[] item, int attemptNumber) throws IOException {
    if (isClosed.get()) {
      logger.log("Refusing to write to storage after being closed.");
      return false;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log("Max number of attempts to write buffered data exceeded.", WARNING);
      return false;
    }
    if (writableFile == null) {
      writableFile = folderManager.createWritableFile();
      logger.log("Created new writableFile: " + writableFile);
    }
    WritableResult result = writableFile.append(item);
    if (result != WritableResult.SUCCEEDED) {
      // Retry with new file
      writableFile = null;
      return write(item, ++attemptNumber);
    }
    return true;
  }

  /**
   * Attempts to read an item from a ready-to-read file.
   *
   * @param processing Is passed over to {@link ReadableFile#readAndProcess(Function)}.
   * @throws IOException If an unexpected error happens.
   */
  public ReadableResult readAndProcess(Function<byte[], Boolean> processing) throws IOException {
    return readAndProcess(processing, 1);
  }

  private ReadableResult readAndProcess(Function<byte[], Boolean> processing, int attemptNumber)
      throws IOException {
    if (isClosed.get()) {
      logger.log("Refusing to read from storage after being closed.");
      return ReadableResult.FAILED;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      logger.log("Maximum number of attempts to read and process buffered data exceeded.", WARNING);
      return ReadableResult.FAILED;
    }
    if (readableFile == null) {
      logger.log("Obtaining a new readableFile from the folderManager.");
      readableFile = folderManager.getReadableFile();
      if (readableFile == null) {
        logger.log("Unable to get or create readable file.");
        return ReadableResult.FAILED;
      }
    }
    logger.log("Attempting to read data from " + readableFile);
    ReadableResult result = readableFile.readAndProcess(processing);
    switch (result) {
      case SUCCEEDED:
      case PROCESSING_FAILED:
        return result;
      default:
        // Retry with new file
        readableFile = null;
        return readAndProcess(processing, ++attemptNumber);
    }
  }

  @Override
  public void close() throws IOException {
    logger.log("Closing disk buffering storage.");
    if (isClosed.compareAndSet(false, true)) {
      if (writableFile != null) {
        writableFile.close();
      }
      if (readableFile != null) {
        readableFile.close();
      }
    }
  }
}
