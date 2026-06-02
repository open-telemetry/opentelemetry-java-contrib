/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileStorageConfiguration;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Writes signal data to a staging file and atomically renames it to its destination on {@link
 * #close()}. Until close, readers in the same directory cannot observe the destination path.
 */
public final class WritableFile implements FileOperations {

  private final File destination;
  private final File staging;
  private final OutputStream out;
  private final FileStorageConfiguration configuration;
  private final Clock clock;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int size;

  public WritableFile(
      File destination,
      File staging,
      long createdTimeMillis,
      FileStorageConfiguration configuration,
      Clock clock)
      throws IOException {
    this.destination = destination;
    this.staging = staging;
    this.out = new FileOutputStream(staging);
    this.configuration = configuration;
    this.clock = clock;
    this.expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForWriteMillis();
  }

  /**
   * Appends a new entry. Closes the file (which atomically promotes its staging file to its final
   * name) and returns {@link WritableResult#FAILED} when the write window has expired or the file
   * is full.
   */
  public synchronized WritableResult append(SignalSerializer<?> marshaler) throws IOException {
    if (isClosed.get()) {
      return WritableResult.FAILED;
    }
    if (hasExpired()) {
      close();
      return WritableResult.FAILED;
    }
    int futureSize = size + marshaler.getBinarySerializedSize();
    if (futureSize > configuration.getMaxFileSize()) {
      close();
      return WritableResult.FAILED;
    }
    marshaler.writeBinaryTo(out);
    size = futureSize;
    return WritableResult.SUCCEEDED;
  }

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
    return destination;
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      out.close();
      if (!staging.renameTo(destination)) {
        throw new IOException("Could not rename " + staging + " to " + destination);
      }
    }
  }

  @Override
  public String toString() {
    return "WritableFile{" + "file=" + destination + '}';
  }

  public void flush() throws IOException {
    out.flush();
  }
}
