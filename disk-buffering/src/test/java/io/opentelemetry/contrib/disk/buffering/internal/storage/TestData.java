package io.opentelemetry.contrib.disk.buffering.internal.storage;

public final class TestData {

  public static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  public static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  public static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;
  public static final int MAX_FILE_SIZE = 100;
  public static final int MAX_FOLDER_SIZE = 300;
  public static final Configuration CONFIGURATION =
      new Configuration(
          MAX_FILE_AGE_FOR_WRITE_MILLIS,
          MIN_FILE_AGE_FOR_READ_MILLIS,
          MAX_FILE_AGE_FOR_READ_MILLIS,
          MAX_FILE_SIZE,
          MAX_FOLDER_SIZE);

  private TestData() {}
}
