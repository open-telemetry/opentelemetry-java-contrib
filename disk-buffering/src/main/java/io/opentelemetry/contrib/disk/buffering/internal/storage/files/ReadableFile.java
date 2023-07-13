/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.DelimitedProtoStreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ReadResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileTransferUtil;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.StorageClock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nullable;

public final class ReadableFile extends StorageFile {
  private final int originalFileSize;
  private final StreamReader reader;
  private final FileTransferUtil fileTransferUtil;
  private final File temporaryFile;
  private final StorageClock clock;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int readBytes = 0;
  @Nullable private ReadResult unconsumedResult;

  public ReadableFile(
      File file, long createdTimeMillis, StorageClock clock, StorageConfiguration configuration)
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
      StorageClock clock,
      StorageConfiguration configuration,
      StreamReader.Factory readerFactory)
      throws IOException {
    super(file);
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
    return clock.now() >= expireTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
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

  private static void copyFile(File from, File to) throws IOException {
    try (InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to, false)) {

      byte[] buffer = new byte[1024];
      int lengthRead;
      while ((lengthRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, lengthRead);
      }
    }
  }
}
