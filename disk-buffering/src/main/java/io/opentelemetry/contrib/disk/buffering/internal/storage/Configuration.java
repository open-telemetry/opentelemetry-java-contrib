package io.opentelemetry.contrib.disk.buffering.internal.storage;

public class Configuration {
  public final long maxFileAgeForWriteInMillis;
  public final long minFileAgeForReadInMillis;
  public final long maxFileAgeForReadInMillis;
  public final int maxFileSize;
  public final int maxFolderSize;

  public Configuration(
      long maxFileAgeForWriteInMillis,
      long minFileAgeForReadInMillis,
      long maxFileAgeForReadInMillis,
      int maxFileSize,
      int maxFolderSize) {
    this.maxFileAgeForWriteInMillis = maxFileAgeForWriteInMillis;
    this.minFileAgeForReadInMillis = minFileAgeForReadInMillis;
    this.maxFileAgeForReadInMillis = maxFileAgeForReadInMillis;
    this.maxFileSize = maxFileSize;
    this.maxFolderSize = maxFolderSize;
  }
}
