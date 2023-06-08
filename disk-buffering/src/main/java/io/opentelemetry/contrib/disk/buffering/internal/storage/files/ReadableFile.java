package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants.NEW_LINE_BYTES_SIZE;

import io.opentelemetry.contrib.disk.buffering.internal.storage.Configuration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoMoreLinesToReadException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.TemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class ReadableFile extends StorageFile {
  private final int originalFileSize;
  private final BufferedReader bufferedReader;
  private final FileChannel tempInChannel;
  private final File temporaryFile;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int readBytes = 0;

  public ReadableFile(
      File file, long createdTimeMillis, TimeProvider timeProvider, Configuration configuration)
      throws IOException {
    this(file, createdTimeMillis, timeProvider, configuration, TemporaryFileProvider.INSTANCE);
  }

  public ReadableFile(
      File file,
      long createdTimeMillis,
      TimeProvider timeProvider,
      Configuration configuration,
      TemporaryFileProvider temporaryFileProvider)
      throws IOException {
    super(file);
    this.timeProvider = timeProvider;
    expireTimeMillis = createdTimeMillis + configuration.maxFileAgeForReadInMillis;
    originalFileSize = (int) file.length();
    temporaryFile = temporaryFileProvider.createTemporaryFile(file.getName());
    Files.copy(file.toPath(), temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    FileInputStream tempInputStream = new FileInputStream(temporaryFile);
    tempInChannel = tempInputStream.getChannel();
    bufferedReader =
        new BufferedReader(new InputStreamReader(tempInputStream, StandardCharsets.UTF_8));
  }

  /**
   * Reads the next line available in the file and provides it to a {@link Function consumer} which
   * will determine whether to remove the provided line or not.
   *
   * @param consumer - A function that receives the line that has been read and returns a boolean.
   *     If the consumer function returns TRUE, then the provided line will be deleted from the
   *     source file. If the function returns FALSE, no changes will be applied to the source file.
   * @throws ReadingTimeoutException If the configured reading time for the file has ended.
   * @throws NoMoreLinesToReadException If there are no more lines to be read from the file.
   * @throws ResourceClosedException If it's closed.
   */
  public synchronized void readLine(Function<byte[], Boolean> consumer) throws IOException {
    if (isClosed.get()) {
      throw new ResourceClosedException();
    }
    if (hasExpired()) {
      throw new ReadingTimeoutException();
    }
    String lineString = bufferedReader.readLine();
    if (lineString == null) {
      throw new NoMoreLinesToReadException();
    }
    byte[] line = lineString.getBytes(StandardCharsets.UTF_8);
    if (consumer.apply(line)) {
      readBytes += line.length + NEW_LINE_BYTES_SIZE;
      try (FileOutputStream out = new FileOutputStream(file, false)) {
        tempInChannel.transferTo(readBytes, originalFileSize - readBytes, out.getChannel());
      }
    }
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
      bufferedReader.close();
      temporaryFile.delete();
    }
  }
}
