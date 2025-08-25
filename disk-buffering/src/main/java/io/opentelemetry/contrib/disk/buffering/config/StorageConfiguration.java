/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.config;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.auto.value.AutoValue;
import java.io.File;

/** Defines how the storage should be managed. */
@AutoValue
public abstract class StorageConfiguration {

  /** The root storage location for buffered telemetry. */
  public abstract File getRootDir();

  /** Returns true if the storage has been configured with debug verbosity enabled. */
  public abstract boolean isDebugEnabled();

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

  public static StorageConfiguration getDefault(File rootDir) {
    return builder().setRootDir(rootDir).build();
  }

  public static Builder builder() {
    return new AutoValue_StorageConfiguration.Builder()
        .setMaxFileSize(1024 * 1024) // 1MB
        .setMaxFolderSize(10 * 1024 * 1024) // 10MB
        .setMaxFileAgeForWriteMillis(SECONDS.toMillis(30))
        .setMinFileAgeForReadMillis(SECONDS.toMillis(33))
        .setMaxFileAgeForReadMillis(HOURS.toMillis(18))
        .setDebugEnabled(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMaxFileAgeForWriteMillis(long value);

    public abstract Builder setMinFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileSize(int value);

    public abstract Builder setMaxFolderSize(int value);

    public abstract Builder setRootDir(File rootDir);

    public abstract Builder setDebugEnabled(boolean debugEnabled);

    public abstract StorageConfiguration build();
  }
}
