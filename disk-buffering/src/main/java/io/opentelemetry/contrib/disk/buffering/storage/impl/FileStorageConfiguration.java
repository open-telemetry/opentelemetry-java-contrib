/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.impl;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.auto.value.AutoValue;

/** Defines how the storage should be managed. */
@AutoValue
public abstract class FileStorageConfiguration {

  /** The max amount of time a file can receive new data. */
  public abstract long getMaxFileAgeForWriteMillis();

  /**
   * The min amount of time needed to pass before reading from a file. Historically this was the
   * mechanism that prevented the reader from observing a file the writer was still appending to. As
   * of the rename-on-close design, finalized files are made visible to the reader atomically once
   * they are rolled, so this value is no longer required for correctness and defaults to {@code 0}.
   * The setting is kept for callers that still want to delay reads of newly-rolled files (e.g. to
   * encourage larger batches before iteration).
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

  /**
   * Whether to automatically delete items from disk during iteration. When true (the default),
   * items are removed from disk as the iterator advances. When false, items remain on disk until
   * explicitly removed via {@link java.util.Iterator#remove()}.
   */
  public abstract boolean getDeleteItemsOnIteration();

  public static FileStorageConfiguration getDefault() {
    return builder().build();
  }

  public static Builder builder() {
    return new AutoValue_FileStorageConfiguration.Builder()
        .setMaxFileSize(1024 * 1024) // 1MB
        .setMaxFolderSize(10 * 1024 * 1024) // 10MB
        .setMaxFileAgeForWriteMillis(SECONDS.toMillis(30))
        .setMinFileAgeForReadMillis(0)
        .setMaxFileAgeForReadMillis(HOURS.toMillis(18))
        .setDeleteItemsOnIteration(true);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMaxFileAgeForWriteMillis(long value);

    public abstract Builder setMinFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileSize(int value);

    public abstract Builder setMaxFolderSize(int value);

    public abstract Builder setDeleteItemsOnIteration(boolean value);

    abstract FileStorageConfiguration autoBuild();

    public final FileStorageConfiguration build() {
      FileStorageConfiguration config = autoBuild();
      checkNonNegative("maxFileAgeForWriteMillis", config.getMaxFileAgeForWriteMillis());
      checkNonNegative("minFileAgeForReadMillis", config.getMinFileAgeForReadMillis());
      checkNonNegative("maxFileAgeForReadMillis", config.getMaxFileAgeForReadMillis());
      checkNonNegative("maxFileSize", config.getMaxFileSize());
      checkNonNegative("maxFolderSize", config.getMaxFolderSize());
      return config;
    }

    private static void checkNonNegative(String name, long value) {
      if (value < 0) {
        throw new IllegalArgumentException(name + " must be >= 0, got " + value);
      }
    }
  }
}
