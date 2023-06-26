/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoContentAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ReadResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class ReadableFile extends StorageFile {
  private final int originalFileSize;
  private final StreamReader reader;
  private final FileChannel tempInChannel;
  private final File temporaryFile;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int readBytes = 0;

  public ReadableFile(
      File file,
      long createdTimeMillis,
      TimeProvider timeProvider,
      StorageConfiguration configuration,
      StreamReader.Factory readerFactory)
      throws IOException {
    super(file);
    this.timeProvider = timeProvider;
    expireTimeMillis = createdTimeMillis + configuration.getMaxFileAgeForReadMillis();
    originalFileSize = (int) file.length();
    temporaryFile = configuration.getTemporaryFileProvider().createTemporaryFile(file.getName());
    copyFile(file, temporaryFile);
    FileInputStream tempInputStream = new FileInputStream(temporaryFile);
    tempInChannel = tempInputStream.getChannel();
    reader = readerFactory.create(tempInputStream);
  }

  /**
   * Reads the next line available in the file and provides it to a {@link Function consumer} which
   * will determine whether to remove the provided line or not.
   *
   * @param consumer - A function that receives the line that has been read and returns a boolean.
   *     If the consumer function returns TRUE, then the provided line will be deleted from the
   *     source file. If the function returns FALSE, no changes will be applied to the source file.
   * @throws ReadingTimeoutException If the configured reading time for the file has ended.
   * @throws NoContentAvailableException If there is no content to be read from the file.
   * @throws ResourceClosedException If it's closed.
   */
  public synchronized void readLine(Function<byte[], Boolean> consumer) throws IOException {
    if (isClosed.get()) {
      throw new ResourceClosedException();
    }
    if (hasExpired()) {
      throw new ReadingTimeoutException();
    }
    ReadResult read = reader.read();
    if (read == null) {
      cleanUp();
      throw new NoContentAvailableException();
    }
    if (consumer.apply(read.content)) {
      readBytes += read.length;
      try (FileOutputStream out = new FileOutputStream(file, false)) {
        int amountOfBytesToTransfer = originalFileSize - readBytes;
        if (amountOfBytesToTransfer > 0) {
          tempInChannel.transferTo(readBytes, amountOfBytesToTransfer, out.getChannel());
        } else {
          cleanUp();
        }
      }
    }
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
    return timeProvider.getSystemCurrentTimeMillis() >= expireTimeMillis;
  }

  @Override
  public synchronized boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      reader.close();
      temporaryFile.delete();
    }
  }

  private static void copyFile(File from, File to) throws IOException {
    try (InputStream in = new BufferedInputStream(new FileInputStream(from));
        OutputStream out = new FileOutputStream(to, false)) {

      byte[] buffer = new byte[1024];
      int lengthRead;
      while ((lengthRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, lengthRead);
      }
    }
  }
}
