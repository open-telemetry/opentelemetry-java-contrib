package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants.NEW_LINE_BYTES;

import io.opentelemetry.contrib.disk.buffering.internal.storage.Configuration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.WritingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WritableFile extends StorageFile {
  private final Configuration configuration;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private final BufferedOutputStream out;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private int size;

  public WritableFile(
      File file, long createdTimeMillis, Configuration configuration, TimeProvider timeProvider)
      throws IOException {
    super(file);
    this.configuration = configuration;
    this.timeProvider = timeProvider;
    expireTimeMillis = createdTimeMillis + configuration.maxFileAgeForWriteInMillis;
    size = (int) file.length();
    out = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
  }

  /**
   * Adds a new line to the file. If {@link WritingTimeoutException} or {@link
   * NoSpaceAvailableException} are thrown, the file stream is closed with the contents available in
   * the buffer before attempting to append the new data.
   *
   * @param data - The new data line to add.
   * @throws IllegalStateException If it's closed.
   * @throws WritingTimeoutException If the configured writing time for the file has ended.
   * @throws NoSpaceAvailableException If the configured max file size has been reached.
   */
  public synchronized void append(byte[] data) throws IOException {
    if (isClosed.get()) {
      throw new IllegalStateException();
    }
    if (hasExpired()) {
      close();
      throw new WritingTimeoutException();
    }
    int futureSize = size + data.length + NEW_LINE_BYTES.length;
    if (futureSize > configuration.maxFileSize) {
      close();
      throw new NoSpaceAvailableException();
    }
    out.write(data);
    out.write(NEW_LINE_BYTES);
    size = futureSize;
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
  public synchronized void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      out.close();
    }
  }
}
