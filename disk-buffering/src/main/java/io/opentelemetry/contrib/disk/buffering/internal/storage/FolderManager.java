/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.util.ClockBuddy.nowMillis;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileStorageConfiguration;
import io.opentelemetry.sdk.common.Clock;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class FolderManager implements Closeable {
  private final File folder;
  private final Clock clock;
  private final FileStorageConfiguration configuration;
  private final Logger logger = Logger.getLogger(FolderManager.class.getName());
  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
  private static final String STAGING_SUFFIX = ".tmp";
  @Nullable private ReadableFile currentReadableFile;
  @Nullable private WritableFile currentWritableFile;

  public static FolderManager create(
      File destinationDir, FileStorageConfiguration configuration, Clock clock) {
    if (destinationDir.isFile()) {
      throw new IllegalArgumentException("destinationDir must be a directory");
    }
    if (!destinationDir.exists()) {
      if (!destinationDir.mkdirs()) {
        throw new IllegalStateException("Could not create dir: " + destinationDir);
      }
    }
    return new FolderManager(destinationDir, configuration, clock);
  }

  public FolderManager(File folder, FileStorageConfiguration configuration, Clock clock) {
    this.folder = folder;
    this.configuration = configuration;
    this.clock = clock;
    recoverOrphanTempFiles();
  }

  private void recoverOrphanTempFiles() {
    File[] existingFiles = folder.listFiles();
    if (existingFiles == null) {
      return;
    }
    for (File file : existingFiles) {
      String name = file.getName();
      if (!file.isFile() || !name.endsWith(STAGING_SUFFIX)) {
        continue;
      }
      File target = new File(folder, name.substring(0, name.length() - STAGING_SUFFIX.length()));
      if (!NUMBER_PATTERN.matcher(target.getName()).matches()) {
        continue;
      }
      if (target.exists()) {
        if (!file.delete()) {
          logger.warning("Could not delete duplicate orphan temp file: '" + file.getName() + "'");
        }
        continue;
      }
      if (file.renameTo(target)) {
        logger.fine(
            "Recovered orphan temp file: '" + file.getName() + "' -> '" + target.getName() + "'");
      } else {
        logger.warning("Could not promote orphan temp file: '" + file.getName() + "'");
      }
    }
  }

  static class CacheFile {
    private final File file;
    private final long createdTimeMillis;

    CacheFile(File file, long createdTimeMillis) {
      this.file = file;
      this.createdTimeMillis = createdTimeMillis;
    }

    long getCreatedTimeMillis() {
      return createdTimeMillis;
    }
  }

  @Override
  public void close() throws IOException {
    closeCurrentFiles();
  }

  @Nullable
  public synchronized ReadableFile getReadableFile(Predicate<CacheFile> excludeFiles)
      throws IOException {
    currentReadableFile = null;
    CacheFile selectedFile = selectReadableFile(listCacheFiles(excludeFiles));
    if (selectedFile == null) {
      if (closeFileIfExpired()) {
        selectedFile = selectReadableFile(listCacheFiles(excludeFiles));
      }
    }
    if (selectedFile != null) {
      currentReadableFile =
          new ReadableFile(selectedFile.file, selectedFile.createdTimeMillis, clock, configuration);
      return currentReadableFile;
    }
    return null;
  }

  /*
   * If the current writable file has expired, close it and return true.
   * This allows to have a readable file without waiting for the next write to trigger the check.
   */
  private boolean closeFileIfExpired() throws IOException {
    if (currentWritableFile == null || currentWritableFile.isClosed()) {
      return false;
    }
    if (!currentWritableFile.hasExpired()) {
      return false;
    }
    currentWritableFile.close();
    return true;
  }

  @NotNull
  public synchronized WritableFile createWritableFile() throws IOException {
    long systemCurrentTimeMillis = nowMillis(clock);
    File[] existingFiles = folder.listFiles();
    if (existingFiles != null) {
      File[] cacheFiles = listNumericFiles(existingFiles);
      if (purgeExpiredFilesIfAny(cacheFiles, systemCurrentTimeMillis) == 0) {
        removeOldestFileIfSpaceIsNeeded(cacheFiles);
      }
    }
    File destination = new File(folder, String.valueOf(systemCurrentTimeMillis));
    File staging = new File(folder, destination.getName() + STAGING_SUFFIX);
    currentWritableFile =
        new WritableFile(destination, staging, systemCurrentTimeMillis, configuration, clock);
    return currentWritableFile;
  }

  public synchronized void clear() throws IOException {
    closeCurrentFiles();
    List<File> undeletedFiles = new ArrayList<>();

    File[] files = folder.listFiles();
    if (files == null) {
      throw new IOException("Could not list files in " + folder);
    }
    for (File file : files) {
      if (!file.delete()) {
        undeletedFiles.add(file);
      }
    }

    if (!undeletedFiles.isEmpty()) {
      throw new IOException("Could not delete files " + undeletedFiles);
    }
  }

  private List<CacheFile> listCacheFiles(Predicate<CacheFile> exclude) {
    File[] existingFiles = folder.listFiles();
    if (existingFiles == null) {
      return Collections.emptyList();
    }
    ArrayList<CacheFile> files = new ArrayList<>();
    for (File file : existingFiles) {
      CacheFile cacheFile = fileToCacheFile(file);
      if (cacheFile != null && !exclude.test(cacheFile)) {
        files.add(cacheFile);
      }
    }
    return Collections.unmodifiableList(files);
  }

  private File[] listNumericFiles(File[] files) {
    List<File> cacheFiles = new ArrayList<>();
    for (File file : files) {
      if (NUMBER_PATTERN.matcher(file.getName()).matches()) {
        cacheFiles.add(file);
      } else if (logger.isLoggable(Level.FINER)) {
        logger.finer("Skipping non-cache file: '" + file.getName() + "'");
      }
    }
    return cacheFiles.toArray(new File[0]);
  }

  @Nullable
  private CacheFile fileToCacheFile(File file) {
    String fileName = file.getName();
    if (!NUMBER_PATTERN.matcher(fileName).matches()) {
      if (logger.isLoggable(Level.FINER)) {
        logger.finer("Invalid cache file name: '" + fileName + "'");
      }
      return null;
    }
    return new CacheFile(file, Long.parseLong(fileName));
  }

  @Nullable
  private CacheFile selectReadableFile(List<CacheFile> files) throws IOException {
    if (files.isEmpty()) {
      return null;
    }

    long currentTime = nowMillis(clock);
    CacheFile oldestFileAvailable = null;
    long oldestFileCreationTimeMillis = 0;
    for (CacheFile existingFile : files) {
      long existingFileCreationTimeMillis = existingFile.createdTimeMillis;
      if (isReadyToBeRead(currentTime, existingFileCreationTimeMillis)
          && !hasExpiredForReading(currentTime, existingFileCreationTimeMillis)) {
        if (oldestFileAvailable == null
            || existingFileCreationTimeMillis < oldestFileCreationTimeMillis) {
          oldestFileCreationTimeMillis = existingFileCreationTimeMillis;
          oldestFileAvailable = existingFile;
        }
      }
    }

    // Checking if the oldest available file is currently the writable file.
    if (oldestFileAvailable != null
        && currentWritableFile != null
        && oldestFileAvailable.file.equals(currentWritableFile.getFile())) {
      currentWritableFile.close();
    }
    return oldestFileAvailable;
  }

  private int purgeExpiredFilesIfAny(File[] existingFiles, long currentTimeMillis)
      throws IOException {
    int filesDeleted = 0;
    for (File existingFile : existingFiles) {
      String fileName = existingFile.getName();
      if (hasExpiredForReading(currentTimeMillis, Long.parseLong(fileName))) {
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

  private synchronized void closeCurrentFiles() throws IOException {
    if (currentReadableFile != null) {
      currentReadableFile.close();
    }
    if (currentWritableFile != null) {
      currentWritableFile.close();
    }
  }
}
