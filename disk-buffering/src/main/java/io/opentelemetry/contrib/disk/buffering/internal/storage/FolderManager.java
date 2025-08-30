/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.sdk.common.Clock;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class FolderManager implements Closeable {
  private final File folder;
  private final Clock clock;
  private final StorageConfiguration configuration;
  @Nullable private ReadableFile currentReadableFile;
  @Nullable private WritableFile currentWritableFile;

  public FolderManager(File folder, StorageConfiguration configuration, Clock clock) {
    this.folder = folder;
    this.configuration = configuration;
    this.clock = clock;
  }

  @Override
  public void close() throws IOException {
    if (currentReadableFile != null) {
      currentReadableFile.close();
    }
    if (currentWritableFile != null) {
      currentWritableFile.close();
    }
  }

  @Nullable
  public synchronized ReadableFile getReadableFile() throws IOException {
    currentReadableFile = null;
    File readableFile = findReadableFile();
    if (readableFile != null) {
      currentReadableFile =
          new ReadableFile(
              readableFile, Long.parseLong(readableFile.getName()), clock, configuration);
      return currentReadableFile;
    }
    return null;
  }

  @NotNull
  public synchronized WritableFile createWritableFile() throws IOException {
    long systemCurrentTimeMillis = nowMillis(clock);
    File[] existingFiles = folder.listFiles();
    if (existingFiles != null) {
      if (purgeExpiredFilesIfAny(existingFiles, systemCurrentTimeMillis) == 0) {
        removeOldestFileIfSpaceIsNeeded(existingFiles);
      }
    }
    File file = new File(folder, String.valueOf(systemCurrentTimeMillis));
    currentWritableFile = new WritableFile(file, systemCurrentTimeMillis, configuration, clock);
    return currentWritableFile;
  }

  @Nullable
  private File findReadableFile() throws IOException {
    long currentTime = nowMillis(clock);
    File[] existingFiles = folder.listFiles();
    File oldestFileAvailable = null;
    long oldestFileCreationTimeMillis = 0;
    if (existingFiles != null) {
      for (File existingFile : existingFiles) {
        long existingFileCreationTimeMillis = Long.parseLong(existingFile.getName());
        if (isReadyToBeRead(currentTime, existingFileCreationTimeMillis)
            && !hasExpiredForReading(currentTime, existingFileCreationTimeMillis)) {
          if (oldestFileAvailable == null
              || existingFileCreationTimeMillis < oldestFileCreationTimeMillis) {
            oldestFileCreationTimeMillis = existingFileCreationTimeMillis;
            oldestFileAvailable = existingFile;
          }
        }
      }
    }
    // Checking if the oldest available file is currently the writable file.
    if (oldestFileAvailable != null
        && currentWritableFile != null
        && oldestFileAvailable.equals(currentWritableFile.getFile())) {
      currentWritableFile.close();
    }
    return oldestFileAvailable;
  }

  private int purgeExpiredFilesIfAny(File[] existingFiles, long currentTimeMillis)
      throws IOException {
    int filesDeleted = 0;
    for (File existingFile : existingFiles) {
      if (hasExpiredForReading(currentTimeMillis, Long.parseLong(existingFile.getName()))) {
        if (currentReadableFile != null && existingFile.equals(currentReadableFile.getFile())) {
          currentReadableFile.close();
        }
        if (existingFile.delete()) {
          filesDeleted++;
        }
      }
    }
    return filesDeleted;
  }

  private void removeOldestFileIfSpaceIsNeeded(File[] existingFiles) throws IOException {
    if (existingFiles.length > 0) {
      if (isNeededToClearSpaceForNewFile(existingFiles)) {
        File oldest = getOldest(existingFiles);
        if (currentReadableFile != null && oldest.equals(currentReadableFile.getFile())) {
          currentReadableFile.close();
        }
        if (!oldest.delete()) {
          throw new IOException("Could not delete the file: " + oldest);
        }
      }
    }
  }

  private static File getOldest(File[] existingFiles) {
    File oldest = null;
    for (File existingFile : existingFiles) {
      if (oldest == null || existingFile.getName().compareTo(oldest.getName()) < 0) {
        oldest = existingFile;
      }
    }
    return Objects.requireNonNull(oldest);
  }

  private boolean isNeededToClearSpaceForNewFile(File[] existingFiles) {
    int currentFolderSize = 0;
    for (File existingFile : existingFiles) {
      currentFolderSize += (int) existingFile.length();
    }
    return (currentFolderSize + configuration.getMaxFileSize()) > configuration.getMaxFolderSize();
  }

  private boolean isReadyToBeRead(long currentTimeMillis, long createdTimeInMillis) {
    return currentTimeMillis >= (createdTimeInMillis + configuration.getMinFileAgeForReadMillis());
  }

  private boolean hasExpiredForReading(long systemCurrentTimeMillis, long createdTimeInMillis) {
    return systemCurrentTimeMillis
        > (createdTimeInMillis + configuration.getMaxFileAgeForReadMillis());
  }
}
