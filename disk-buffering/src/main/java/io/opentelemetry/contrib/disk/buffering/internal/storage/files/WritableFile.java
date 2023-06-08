package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import io.opentelemetry.contrib.disk.buffering.internal.storage.Configuration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class WritableFile extends StorageFile {
  private final Configuration configuration;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private final byte[] newLineBytes;
  private final BufferedOutputStream out;
  private int size;

  public WritableFile(
      File file, long createdTimeMillis, Configuration configuration, TimeProvider timeProvider)
      throws IOException {
    super(file);
    this.configuration = configuration;
    this.timeProvider = timeProvider;
    expireTimeMillis = createdTimeMillis + configuration.maxFileAgeForWriteInMillis;
    size = (int) file.length();
    newLineBytes = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
    out = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
  }

  public synchronized void append(byte[] data) throws IOException {
    int futureSize = size + data.length + newLineBytes.length;
    if (futureSize > configuration.maxFileSize) {
      close();
      throw new NoSpaceAvailableException();
    }
    out.write(data);
    out.write(newLineBytes);
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
    out.close();
  }
}
