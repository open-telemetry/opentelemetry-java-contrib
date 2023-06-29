/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WritableFile extends StorageFile {
  private final StorageConfiguration configuration;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private final OutputStream out;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int size;

  public WritableFile(
      File file,
      long createdTimeMillis,
      StorageConfiguration configuration,
      TimeProvider timeProvider)
      throws IOException {
    super(file);
    this.configuration = configuration;
    this.timeProvider = timeProvider;
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
    return timeProvider.getSystemCurrentTimeMillis() >= expireTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      out.close();
    }
  }
}
