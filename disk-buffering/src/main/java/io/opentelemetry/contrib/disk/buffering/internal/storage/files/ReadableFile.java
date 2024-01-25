/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.DelimitedProtoStreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ReadResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileTransferUtil;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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
  private final File file;
  private final int originalFileSize;
  private final StreamReader reader;
  private final FileTransferUtil fileTransferUtil;
  private final File temporaryFile;
  private final Clock clock;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int readBytes = 0;
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
      File file,
      long createdTimeMillis,
      Clock clock,
      StorageConfiguration configuration,
      StreamReader.Factory readerFactory)
      throws IOException {
    this.file = file;
    this.clock = clock;
    expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForReadMillis();
    originalFileSize = (int) file.length();
    temporaryFile = configuration.getTemporaryFileProvider().createTemporaryFile(file.getName());
    copyFile(file, temporaryFile);
    FileInputStream tempInputStream = new FileInputStream(temporaryFile);
    fileTransferUtil = new FileTransferUtil(tempInputStream, file);
    reader = readerFactory.create(tempInputStream);
  }

  /**
   * Reads the next line available in the file and provides it to a {@link Function processing}
   * which will determine whether to remove the provided line or not.
   *
   * @param processing - A function that receives the line that has been read and returns a boolean.
   *     If the processing function returns TRUE, then the provided line will be deleted from the
   *     source file. If the function returns FALSE, no changes will be applied to the source file.
   */
  public synchronized ReadableResult readAndProcess(Function<byte[], Boolean> processing)
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
    if (processing.apply(read.content)) {
      unconsumedResult = null;
      readBytes += read.totalReadLength;
      int amountOfBytesToTransfer = originalFileSize - readBytes;
      if (amountOfBytesToTransfer > 0) {
        fileTransferUtil.transferBytes(readBytes, amountOfBytesToTransfer);
      } else {
        cleanUp();
      }
      return ReadableResult.SUCCEEDED;
    } else {
      unconsumedResult = read;
      return ReadableResult.PROCESSING_FAILED;
    }
  }

  @Nullable
  private ReadResult readNextItem() throws IOException {
    if (unconsumedResult != null) {
      return unconsumedResult;
    }
    return reader.read();
  }

  private void cleanUp() throws IOException {
    file.delete();
    close();
  }

  @Override
  public long getSize() {
    return originalFileSize;
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
      unconsumedResult = null;
      fileTransferUtil.close();
      reader.close();
      temporaryFile.delete();
    }
  }

  /**
   * This is needed instead of using Files.copy in order to keep it compatible with Android api <
   * 26.
   */
  private static void copyFile(File from, File to) throws IOException {
    try (InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to)) {

      byte[] buffer = new byte[1024];
      int lengthRead;
      while ((lengthRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, lengthRead);
      }
    }
  }
}
