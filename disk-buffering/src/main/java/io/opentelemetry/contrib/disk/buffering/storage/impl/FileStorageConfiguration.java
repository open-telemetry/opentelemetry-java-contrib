/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.impl;

import com.google.auto.value.AutoValue;
import java.util.concurrent.TimeUnit;

/** Defines how the storage should be managed. */
@AutoValue
public abstract class FileStorageConfiguration {

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

  public static FileStorageConfiguration getDefault() {
    return builder().build();
  }

  public static Builder builder() {
    return new AutoValue_FileStorageConfiguration.Builder()
        .setMaxFileSize(1024 * 1024) // 1MB
        .setMaxFolderSize(10 * 1024 * 1024) // 10MB
        .setMaxFileAgeForWriteMillis(TimeUnit.SECONDS.toMillis(30))
        .setMinFileAgeForReadMillis(TimeUnit.SECONDS.toMillis(33))
        .setMaxFileAgeForReadMillis(TimeUnit.HOURS.toMillis(18));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMaxFileAgeForWriteMillis(long value);

    public abstract Builder setMinFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileSize(int value);

    public abstract Builder setMaxFolderSize(int value);

    abstract FileStorageConfiguration autoBuild();

    public final FileStorageConfiguration build() {
      FileStorageConfiguration configuration = autoBuild();
      if (configuration.getMinFileAgeForReadMillis()
          <= configuration.getMaxFileAgeForWriteMillis()) {
        throw new IllegalArgumentException(
            "The configured max file age for writing must be lower than the configured min file age for reading");
      }
      return configuration;
    }
  }
}
