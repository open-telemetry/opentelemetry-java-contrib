package io.opentelemetry.contrib.disk.buffering.internal.storage;

import com.google.errorprone.annotations.DoNotCall;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import javax.annotation.Nullable;

public class FileProvider {
  private final File rootDir;
  private final TimeProvider timeProvider;
  private final Configuration configuration;

  public FileProvider(File rootDir, TimeProvider timeProvider, Configuration configuration) {
    this.rootDir = rootDir;
    this.timeProvider = timeProvider;
    this.configuration = configuration;
  }

  @DoNotCall
  public final FileHolder getReadableFile() {
    throw new UnsupportedOperationException();
  }

  public FileHolder getWritableFile() {
    long systemCurrentTimeMillis = timeProvider.getSystemCurrentTimeMillis();
    File existingFile = findExistingUnexpiredFile(systemCurrentTimeMillis);
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
        if (hasExpired(currentTimeMillis, Long.parseLong(existingFile.getName()))) {
          existingFile.delete();
        }
      }
    }
  }

  @Nullable
  private File findExistingUnexpiredFile(long systemCurrentTimeMillis) {
    File[] existingFiles = rootDir.listFiles();
    if (existingFiles != null) {
      for (File existingFile : existingFiles) {
        if (hasNotExpired(systemCurrentTimeMillis, Long.parseLong(existingFile.getName()))) {
          return existingFile;
        }
      }
    }
    return null;
  }

  private boolean hasExpired(long systemCurrentTimeMillis, long createdTimeInMillis) {
    return !hasNotExpired(systemCurrentTimeMillis, createdTimeInMillis);
  }

  private boolean hasNotExpired(long systemCurrentTimeMillis, long createdTimeInMillis) {
    return systemCurrentTimeMillis < (createdTimeInMillis + configuration.maxFileAgeInMillis);
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
