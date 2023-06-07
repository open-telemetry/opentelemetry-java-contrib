package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import io.opentelemetry.contrib.disk.buffering.internal.storage.Configuration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;

public final class WritableFile extends StorageFile {
  private final Configuration configuration;
  private final TimeProvider timeProvider;
  private final long expireTimeMillis;
  private int size;

  public WritableFile(File file, Configuration configuration, TimeProvider timeProvider) {
    super(file);
    this.configuration = configuration;
    this.timeProvider = timeProvider;
    long createdTimeMillis = Long.parseLong(file.getName());
    expireTimeMillis = createdTimeMillis + configuration.maxFileAgeForWriteInMillis;
    size = (int) file.length();
  }

  public synchronized void append(byte[] data) {
    size += data.length;
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public synchronized boolean isValid() {
    return writingTimeHasNotExpired() && hasNotReachedMaxSize();
  }

  private boolean hasNotReachedMaxSize() {
    return size < configuration.maxFileSize;
  }

  private boolean writingTimeHasNotExpired() {
    return expireTimeMillis > timeProvider.getSystemCurrentTimeMillis();
  }

  @Override
  public synchronized void close() {}
}
