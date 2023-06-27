/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.MaxAttemptsReachedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoContentAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.WritingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
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
   * @throws MaxAttemptsReachedException If there are too many unsuccessful retries.
   * @throws ResourceClosedException If it's closed.
   * @throws IOException If an unexpected error happens.
   */
  public void write(byte[] line) throws IOException {
    write(line, 1);
  }

  private void write(byte[] line, int attemptNumber) throws IOException {
    if (isClosed.get()) {
      throw new ResourceClosedException();
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      throw new MaxAttemptsReachedException();
    }
    if (writableFile == null) {
      writableFile = folderManager.createWritableFile();
    }
    try {
      writableFile.append(line);
    } catch (WritingTimeoutException | NoSpaceAvailableException | ResourceClosedException e) {
      // Retry with new file
      writableFile = null;
      write(line, ++attemptNumber);
    }
  }

  /**
   * Attempts to read a line from a ready-to-read file.
   *
   * @param consumer Is passed over to {@link ReadableFile#readItem(Function)}.
   * @return TRUE if data was found and read, FALSE if there is no data available to read.
   * @throws MaxAttemptsReachedException If there are too many unsuccessful retries.
   * @throws ResourceClosedException If it's closed.
   * @throws IOException If an unexpected error happens.
   */
  public boolean read(Function<byte[], Boolean> consumer) throws IOException {
    return read(consumer, 1);
  }

  private boolean read(Function<byte[], Boolean> consumer, int attemptNumber) throws IOException {
    if (isClosed.get()) {
      throw new ResourceClosedException();
    }
    if (attemptNumber > MAX_ATTEMPTS) {
      throw new MaxAttemptsReachedException();
    }
    if (readableFile == null) {
      readableFile = folderManager.getReadableFile();
      if (readableFile == null) {
        return false;
      }
    }
    try {
      readableFile.readItem(consumer);
      return true;
    } catch (ReadingTimeoutException | NoContentAvailableException | ResourceClosedException e) {
      // Retry with new file
      readableFile = null;
      return read(consumer, ++attemptNumber);
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
