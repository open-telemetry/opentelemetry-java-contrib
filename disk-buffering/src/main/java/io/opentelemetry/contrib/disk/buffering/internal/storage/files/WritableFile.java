package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants.NEW_LINE_BYTES;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.WritingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
    out = Files.newOutputStream(file.toPath());
  }

  /**
   * Adds a new line to the file. If {@link WritingTimeoutException} or {@link
   * NoSpaceAvailableException} are thrown, the file stream is closed with the contents available in
   * the buffer before attempting to append the new data.
   *
   * @param data - The new data line to add.
   * @throws ResourceClosedException If it's closed.
   * @throws WritingTimeoutException If the configured writing time for the file has ended.
   * @throws NoSpaceAvailableException If the configured max file size has been reached.
   */
  public synchronized void append(byte[] data) throws IOException {
    if (isClosed.get()) {
      throw new ResourceClosedException();
    }
    if (hasExpired()) {
      close();
      throw new WritingTimeoutException();
    }
    int futureSize = size + data.length + NEW_LINE_BYTES.length;
    if (futureSize > configuration.getMaxFileSize()) {
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
