package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;

public final class FileProvider {
  private final File rootDir;
  private final TimeProvider timeProvider;
  private final Configuration configuration;
  @Nullable private WritableFile currentWritableFile;
  //  private ReadableFile currentReadableFile;

  public FileProvider(File rootDir, TimeProvider timeProvider, Configuration configuration) {
    this.rootDir = rootDir;
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

  public synchronized WritableFile getWritableFile() throws IOException {
    if (currentWritableFile != null) {
      return currentWritableFile;
    }
    long systemCurrentTimeMillis = timeProvider.getSystemCurrentTimeMillis();
    File[] existingFiles = rootDir.listFiles();
    if (existingFiles != null) {
      File existingFile = findExistingWritableFile(existingFiles, systemCurrentTimeMillis);
      if (existingFile != null && hasNotReachedMaxSize(existingFile.length())) {
        return createWritableFile(existingFile);
      }
      if (purgeExpiredFilesIfAny(existingFiles, systemCurrentTimeMillis) == 0) {
        removeOldestFileIfSpaceIsNeeded(existingFiles);
      }
    }
    File file = new File(rootDir, String.valueOf(systemCurrentTimeMillis));
    return createWritableFile(file);
  }

  private WritableFile createWritableFile(File existingFile) {
    currentWritableFile = new WritableFile(existingFile);
    return currentWritableFile;
  }

  @Nullable
  private File findReadableFile() {
    long currentTime = timeProvider.getSystemCurrentTimeMillis();
    File[] existingFiles = rootDir.listFiles();
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

  @Nullable
  private File findExistingWritableFile(File[] existingFiles, long systemCurrentTimeMillis) {
    for (File existingFile : existingFiles) {
      if (hasNotExpiredForWriting(
          systemCurrentTimeMillis, Long.parseLong(existingFile.getName()))) {
        return existingFile;
      }
    }
    return null;
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
      if (neededToClearSpaceForNewFile(existingFiles)) {
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

  private boolean neededToClearSpaceForNewFile(File[] existingFiles) {
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

  private boolean hasNotExpiredForWriting(long systemCurrentTimeMillis, long createdTimeInMillis) {
    return systemCurrentTimeMillis
        < (createdTimeInMillis + configuration.maxFileAgeForWriteInMillis);
  }

  private boolean hasNotReachedMaxSize(long fileSize) {
    return fileSize < configuration.maxFileSize;
  }
}