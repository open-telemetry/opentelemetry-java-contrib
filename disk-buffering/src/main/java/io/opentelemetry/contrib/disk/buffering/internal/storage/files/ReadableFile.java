/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.DelimitedProtoStreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileStream;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileStorageConfiguration;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reads items sequentially from a cache file. Items can be explicitly removed after reading via
 * {@link #removeTopItem()}. If not removed, items remain on disk for future reads.
 *
 * <p>More information on the overall storage process in the CONTRIBUTING.md file.
 */
public final class ReadableFile implements FileOperations {
  @Nonnull private final File file;
  private final FileStream fileStream;
  private final StreamReader reader;
  private final Clock clock;
  private final long createdTimeMillis;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  public ReadableFile(
      File file, long createdTimeMillis, Clock clock, FileStorageConfiguration configuration)
      throws IOException {
    this(
        file,
        createdTimeMillis,
        clock,
        configuration,
        DelimitedProtoStreamReader.Factory.getInstance());
  }

  public ReadableFile(
      @Nonnull File file,
      long createdTimeMillis,
      Clock clock,
      FileStorageConfiguration configuration,
      StreamReader.Factory readerFactory)
      throws IOException {
    this.file = file;
    this.clock = clock;
    this.createdTimeMillis = createdTimeMillis;
    expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForReadMillis();
    fileStream = FileStream.create(file);
    reader = readerFactory.create(fileStream);
  }

  /** Reads the next item available in the file. Returns null if no more items or file is closed. */
  @Nullable
  public synchronized byte[] readNext() throws IOException {
    if (isClosed.get()) {
      return null;
    }
    if (hasExpired()) {
      close();
      return null;
    }
    byte[] resultBytes = reader.readNext();
    if (resultBytes == null) {
      close();
      return null;
    }
    return resultBytes;
  }

  @Override
  public synchronized boolean hasExpired() {
    return nowMillis(clock) >= expireTimeMillis;
  }

  public synchronized long getCreatedTimeMillis() {
    if (isClosed.get()) {
      throw new IllegalStateException("File is closed");
    }
    return createdTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
  }

  @Nonnull
  @Override
  public File getFile() {
    return file;
  }

  public synchronized void clear() throws IOException {
    close();
    if (!file.delete()) {
      throw new IOException("Could not delete file: " + file);
    }
  }

  public synchronized void removeTopItem() throws IOException {
    fileStream.truncateTop();
    if (fileStream.size() == 0) {
      clear();
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      reader.close();
    }
  }

  @Override
  public String toString() {
    return "ReadableFile{" + "file=" + file + '}';
  }
}
