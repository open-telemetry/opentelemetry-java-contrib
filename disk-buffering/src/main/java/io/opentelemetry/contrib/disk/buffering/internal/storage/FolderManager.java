package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;

@SuppressWarnings("FieldCanBeLocal") // todo delete
public final class FolderManager {
  private final File folder;
  private final TimeProvider timeProvider;
  private final Configuration configuration;
  @Nullable private WritableFile currentWritableFile;
  //  private ReadableFile currentReadableFile;

  public FolderManager(File folder, TimeProvider timeProvider, Configuration configuration) {
    this.folder = folder;
    this.timeProvider = timeProvider;
    this.configuration = configuration;
  }

  @Nullable
  public synchronized ReadableFile getReadableFile() {
    File readableFile = findReadableFile();
    if (readableFile != null) {
      return new ReadableFile(readableFile);
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
    currentWritableFile = new WritableFile(file, configuration, timeProvider);
    return currentWritableFile;
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

  private int purgeExpiredFilesIfAny(File[] existingFiles, long currentTimeMillis) {
    int filesDeleted = 0;
    for (File existingFile : existingFiles) {
      if (hasExpiredForReading(currentTimeMillis, Long.parseLong(existingFile.getName()))) {
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
    return (currentFolderSize + configuration.maxFileSize) > configuration.maxFolderSize;
  }

  private boolean isReadyToBeRead(long currentTimeMillis, long createdTimeInMillis) {
    return currentTimeMillis >= (createdTimeInMillis + configuration.minFileAgeForReadInMillis);
  }

  private boolean hasExpiredForReading(long systemCurrentTimeMillis, long createdTimeInMillis) {
    return systemCurrentTimeMillis
        > (createdTimeInMillis + configuration.maxFileAgeForReadInMillis);
  }
}
