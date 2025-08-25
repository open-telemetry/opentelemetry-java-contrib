/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.DelimitedProtoStreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ProcessResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ReadResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileStream;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Reads from a file and updates it in parallel in order to avoid re-reading the same items later.
 * The way it does so is by creating a temporary file where all the contents are added during the
 * instantiation of this class. Then, the contents are read from the temporary file, after an item
 * has been read from the temporary file, the original file gets updated to remove the recently read
 * data.
 *
 * <p>More information on the overall storage process in the CONTRIBUTING.md file.
 */
public class ReadableFile implements FileOperations {
  @NotNull private final File file;
  private final FileStream fileStream;
  private final StreamReader reader;
  private final Clock clock;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  @Nullable private ReadResult unconsumedResult;

  public ReadableFile(
      File file, long createdTimeMillis, Clock clock, StorageConfiguration configuration)
      throws IOException {
    this(
        file,
        createdTimeMillis,
        clock,
        configuration,
        DelimitedProtoStreamReader.Factory.getInstance());
  }

  public ReadableFile(
      @NotNull File file,
      long createdTimeMillis,
      Clock clock,
      StorageConfiguration configuration,
      StreamReader.Factory readerFactory)
      throws IOException {
    this.file = file;
    this.clock = clock;
    expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForReadMillis();
    fileStream = FileStream.create(file);
    reader = readerFactory.create(fileStream);
  }

  /**
   * Reads the next line available in the file and provides it to a {@link Function processing}
   * which will determine whether to remove the provided line or not.
   *
   * @param processing - A function that receives the line that has been read and returns a boolean.
   *     If the processing function returns TRUE, then the provided line will be deleted from the
   *     source file. If the function returns FALSE, no changes will be applied to the source file.
   */
  public synchronized ReadableResult readAndProcess(Function<byte[], ProcessResult> processing)
      throws IOException {
    if (isClosed.get()) {
      return ReadableResult.FAILED;
    }
    if (hasExpired()) {
      close();
      return ReadableResult.FAILED;
    }
    ReadResult read = readNextItem();
    if (read == null) {
      cleanUp();
      return ReadableResult.FAILED;
    }
    switch (processing.apply(read.content)) {
      case SUCCEEDED:
        unconsumedResult = null;
        fileStream.truncateTop();
        if (fileStream.size() == 0) {
          cleanUp();
        }
        return ReadableResult.SUCCEEDED;
      case TRY_LATER:
        unconsumedResult = read;
        return ReadableResult.TRY_LATER;
      case CONTENT_INVALID:
        cleanUp();
        return ReadableResult.FAILED;
    }
    return ReadableResult.FAILED;
  }

  @Nullable
  private ReadResult readNextItem() throws IOException {
    if (unconsumedResult != null) {
      return unconsumedResult;
    }
    return reader.readNext();
  }

  @Override
  public synchronized boolean hasExpired() {
    return nowMillis(clock) >= expireTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
  }

  @NotNull
  @Override
  public File getFile() {
    return file;
  }

  private void cleanUp() throws IOException {
    close();
    if (!file.delete()) {
      throw new IOException("Could not delete file: " + file);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      unconsumedResult = null;
      reader.close();
    }
  }

  @Override
  public String toString() {
    return "ReadableFile{" + "file=" + file + '}';
  }
}
