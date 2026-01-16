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
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reads from a file and updates it in parallel in order to avoid re-reading the same items later.
 * The way it does so is by creating a temporary file where all the contents are added during the
 * instantiation of this class. Then, the contents are read from the temporary file, after an item
 * has been read from the temporary file, the original file gets updated to remove the recently read
 * data.
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

  /**
   * Reads the next line available in the file and provides it to a {@link Function processing}
   * which will determine whether to remove the provided line or not.
   */
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

  public long getCreatedTimeMillis() {
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
