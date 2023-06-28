/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nullable;

public final class Storage implements Closeable {
  private final FolderManager folderManager;
  @Nullable private WritableFile writableFile;
  @Nullable private ReadableFile readableFile;
  private static final int MAX_ATTEMPTS = 3;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  public Storage(FolderManager folderManager) {
    this.folderManager = folderManager;
  }

  /**
   * Attempts to write a line into a writable file.
   *
   * @param line - The data that would be appended to the file.
   * @throws IOException If an unexpected error happens.
   */
  public boolean write(byte[] line) throws IOException {
    return write(line, 1);
  }

  private boolean write(byte[] line, int attemptNumber) throws IOException {
    if (isClosed.get()) {
      return false;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      return false;
    }
    if (writableFile == null) {
      writableFile = folderManager.createWritableFile();
    }
    WritableResult result = writableFile.append(line);
    if (result != WritableResult.SUCCEEDED) {
      // Retry with new file
      writableFile = null;
      return write(line, ++attemptNumber);
    }
    return true;
  }

  /**
   * Attempts to read a line from a ready-to-read file.
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
      return ReadableResult.CLOSED;
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      return ReadableResult.NO_CONTENT_AVAILABLE;
    }
    if (readableFile == null) {
      readableFile = folderManager.getReadableFile();
      if (readableFile == null) {
        return ReadableResult.NO_CONTENT_AVAILABLE;
      }
    }
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
