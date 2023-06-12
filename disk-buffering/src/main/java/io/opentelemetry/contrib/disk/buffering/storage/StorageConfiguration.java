package io.opentelemetry.contrib.disk.buffering.storage;

import com.google.auto.value.AutoValue;
import java.util.concurrent.TimeUnit;

/** Defines how the storage should be managed. */
@AutoValue
public abstract class StorageConfiguration {
  /** The max amount of time a file can receive new data. */
  public abstract long getMaxFileAgeForWriteMillis();

  /**
   * The min amount of time needed to pass before reading from a file. This value MUST be greater
   * than getMaxFileAgeForWriteMillis() to make sure the selected file to read is not being written
   * to.
   */
  public abstract long getMinFileAgeForReadMillis();

  /**
   * The max amount of time a file can be read from, which is also the amount of time a file is not
   * considered to be deleted as stale.
   */
  public abstract long getMaxFileAgeForReadMillis();

  /**
   * The max file size, If the getMaxFileAgeForWriteMillis() time value hasn't passed but the file
   * has reached this size, it stops receiving data.
   */
  public abstract int getMaxFileSize();

  /**
   * All the files are stored in a signal-specific folder. This number represents each folder's
   * size, therefore the max amount of cache size for the overall telemetry data would be the sum of
   * the folder sizes of all the signals being stored in disk.
   */
  public abstract int getMaxFolderSize();

  public static StorageConfiguration getDefault() {
    return builder().build();
  }

  public static Builder builder() {
    return new AutoValue_StorageConfiguration.Builder()
        .setMaxFileSize(1024 * 1024) // 1MB
        .setMaxFolderSize(20 * 1024 * 1024) // 20MB
        .setMaxFileAgeForWriteMillis(TimeUnit.SECONDS.toMillis(5))
        .setMinFileAgeForReadMillis(TimeUnit.SECONDS.toMillis(6))
        .setMaxFileAgeForReadMillis(TimeUnit.HOURS.toMillis(18));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMaxFileAgeForWriteMillis(long value);

    public abstract Builder setMinFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileSize(int value);

    public abstract Builder setMaxFolderSize(int value);

    public abstract StorageConfiguration build();
  }
}
