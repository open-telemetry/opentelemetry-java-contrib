package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import javax.annotation.Nullable;

public final class FileProvider {
  private final File rootDir;
  private final TimeProvider timeProvider;
  private final Configuration configuration;

  public FileProvider(File rootDir, TimeProvider timeProvider, Configuration configuration) {
    this.rootDir = rootDir;
    this.timeProvider = timeProvider;
    this.configuration = configuration;
  }

  @Nullable
  public synchronized FileHolder getReadableFile() {
    File readableFile = findReadableFile();
    if (readableFile != null) {
      return new SimpleFileHolder(readableFile);
    }
    return null;
  }

  public synchronized FileHolder getWritableFile() {
    long systemCurrentTimeMillis = timeProvider.getSystemCurrentTimeMillis();
    File existingFile = findExistingWritableFile(systemCurrentTimeMillis);
    if (existingFile != null) {
      return new SimpleFileHolder(existingFile);
    }
    purgeExpiredFilesIfAny(systemCurrentTimeMillis);
    File file = new File(rootDir, String.valueOf(systemCurrentTimeMillis));
    return new SimpleFileHolder(file);
  }

  private void purgeExpiredFilesIfAny(long currentTimeMillis) {
    File[] existingFiles = rootDir.listFiles();
    if (existingFiles != null) {
      for (File existingFile : existingFiles) {
        if (hasExpiredForReading(currentTimeMillis, Long.parseLong(existingFile.getName()))) {
          existingFile.delete();
        }
      }
    }
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
  private File findExistingWritableFile(long systemCurrentTimeMillis) {
    File[] existingFiles = rootDir.listFiles();
    if (existingFiles != null) {
      for (File existingFile : existingFiles) {
        if (hasNotExpiredForWriting(
            systemCurrentTimeMillis, Long.parseLong(existingFile.getName()))) {
          return existingFile;
        }
      }
    }
    return null;
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

  public static final class SimpleFileHolder implements FileHolder {
    private final File file;

    public SimpleFileHolder(File file) {
      this.file = file;
    }

    @Override
    public File getFile() {
      return file;
    }
  }
}
