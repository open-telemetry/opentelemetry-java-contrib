package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;

public final class FolderManager {
  private final File folder;
  private final TimeProvider timeProvider;
  private final StorageConfiguration configuration;
  @Nullable private ReadableFile currentReadableFile;

  public FolderManager(File folder, StorageConfiguration configuration) {
    this(folder, configuration, TimeProvider.get());
  }

  public FolderManager(File folder, StorageConfiguration configuration, TimeProvider timeProvider) {
    this.folder = folder;
    this.configuration = configuration;
    this.timeProvider = timeProvider;
  }

  @Nullable
  public synchronized ReadableFile getReadableFile() throws IOException {
    currentReadableFile = null;
    File readableFile = findReadableFile();
    if (readableFile != null) {
      currentReadableFile =
          new ReadableFile(
              readableFile, Long.parseLong(readableFile.getName()), timeProvider, configuration);
      return currentReadableFile;
    }
    return null;
  }

  public synchronized WritableFile createWritableFile() throws IOException {
    long systemCurrentTimeMillis = timeProvider.getSystemCurrentTimeMillis();
    File[] existingFiles = folder.listFiles();
    if (existingFiles != null) {
      if (purgeExpiredFilesIfAny(existingFiles, systemCurrentTimeMillis) == 0) {
        removeOldestFileIfSpaceIsNeeded(existingFiles);
      }
    }
    File file = new File(folder, String.valueOf(systemCurrentTimeMillis));
    return new WritableFile(file, systemCurrentTimeMillis, configuration, timeProvider);
  }

  @Nullable
  private File findReadableFile() {
    long currentTime = timeProvider.getSystemCurrentTimeMillis();
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
    return oldestFileAvailable;
  }

  private int purgeExpiredFilesIfAny(File[] existingFiles, long currentTimeMillis)
      throws IOException {
    int filesDeleted = 0;
    for (File existingFile : existingFiles) {
      if (hasExpiredForReading(currentTimeMillis, Long.parseLong(existingFile.getName()))) {
        if (currentReadableFile != null && existingFile.equals(currentReadableFile.file)) {
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
        if (currentReadableFile != null && oldest.equals(currentReadableFile.file)) {
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
