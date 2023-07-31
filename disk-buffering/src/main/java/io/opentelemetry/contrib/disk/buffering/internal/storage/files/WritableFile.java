/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WritableFile implements FileOperations {

  private final File file;

  private final StorageConfiguration configuration;
  private final Clock clock;
  private final long expireTimeMillis;
  private final OutputStream out;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int size;

  public WritableFile(
      File file, long createdTimeMillis, StorageConfiguration configuration, Clock clock)
      throws IOException {
    this.file = file;
    this.configuration = configuration;
    this.clock = clock;
    expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForWriteMillis();
    size = (int) file.length();
    out = new FileOutputStream(file);
  }

  /**
   * Adds a new line to the file. If it fails due to expired write time or because the file has
   * reached the configured max size, the file stream is closed with the contents available in the
   * buffer before attempting to append the new data.
   *
   * @param data - The new data line to add.
   */
  public synchronized WritableResult append(byte[] data) throws IOException {
    if (isClosed.get()) {
      return WritableResult.FAILED;
    }
    if (hasExpired()) {
      close();
      return WritableResult.FAILED;
    }
    int futureSize = size + data.length;
    if (futureSize > configuration.getMaxFileSize()) {
      close();
      return WritableResult.FAILED;
    }
    out.write(data);
    size = futureSize;
    return WritableResult.SUCCEEDED;
  }

  @Override
  public synchronized long getSize() {
    return size;
  }

  @Override
  public synchronized boolean hasExpired() {
    return nowMillis(clock) >= expireTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      out.close();
    }
  }
}
