/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.ByteArraySerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderManagerTest {

  @TempDir File rootDir;
  private FolderManager folderManager;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = mock();
    folderManager = new FolderManager(rootDir, TestData.getConfiguration(), clock);
  }

  @AfterEach
  void tearDown() throws Exception {
    folderManager.close();
  }

  @Test
  void createWritableFile_withTimeMillisAsName() throws IOException {
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(1000L));
    WritableFile file = folderManager.createWritableFile();

    assertThat(file.getFile().getName()).isEqualTo("1000");
  }

  @Test
  void clearFiles() throws IOException {
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(1000L));

    // Creating file
    folderManager.createWritableFile();
    assertThat(rootDir.list()).containsExactly("1000");

    // Clear
    folderManager.clear();
    assertThat(rootDir.list()).isEmpty();
  }

  @Test
  void createWritableFile_andRemoveOldestOne_whenTheAvailableFolderSpaceIsNotEnough()
      throws IOException {
    File existingFile1 = new File(rootDir, "1000");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "1100");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    when(clock.now()).thenReturn(1500L);

    WritableFile file = folderManager.createWritableFile();

    assertThat(file.getFile()).isNotEqualTo(existingFile1);
    assertThat(file.getFile()).isNotEqualTo(existingFile2);
    assertThat(file.getFile()).isNotEqualTo(existingFile3);
    assertThat(existingFile2.exists()).isTrue();
    assertThat(existingFile3.exists()).isTrue();
    assertThat(existingFile1.exists()).isFalse();
  }

  @Test
  void closeCurrentlyWritableFile_whenItIsReadyToBeRead_andNoOtherReadableFilesAreAvailable()
      throws IOException {
    long createdFileTime = 1000L;
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(createdFileTime));

    WritableFile writableFile = folderManager.createWritableFile();
    writableFile.append(new ByteArraySerializer(new byte[3]));

    when(clock.now())
        .thenReturn(MILLISECONDS.toNanos(createdFileTime + MIN_FILE_AGE_FOR_READ_MILLIS));

    ReadableFile readableFile = getReadableFile();

    assertThat(readableFile.getFile()).isEqualTo(writableFile.getFile());
    assertThat(writableFile.isClosed()).isTrue();
  }

  @Test
  void
      closeCurrentlyReadableFileIfAny_whenItIsTheOldestOne_andRemoveIt_whenTheAvailableFolderSpaceIsNotEnough()
          throws IOException {
    File existingFile1 = new File(rootDir, "1000");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "1100");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(1000L + MIN_FILE_AGE_FOR_READ_MILLIS));

    ReadableFile readableFile = getReadableFile();
    assertThat(readableFile.getFile()).isEqualTo(existingFile1);

    folderManager.createWritableFile();

    assertThat(existingFile2.exists()).isTrue();
    assertThat(existingFile3.exists()).isTrue();
    assertThat(existingFile1.exists()).isFalse();
    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void createWritableFile_andDoNotRemoveOldestOne_ifAtLeastOneExpiredFileIsPurged()
      throws IOException {
    File existingFile1 = new File(rootDir, "1100");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "900");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(11_000L));

    WritableFile file = folderManager.createWritableFile();

    assertThat(file.getFile()).isNotEqualTo(existingFile1);
    assertThat(file.getFile()).isNotEqualTo(existingFile2);
    assertThat(file.getFile()).isNotEqualTo(existingFile3);
    assertThat(existingFile2.exists()).isTrue();
    assertThat(existingFile1.exists()).isTrue();
    assertThat(existingFile3.exists()).isFalse();
  }

  @Test
  void purgeExpiredForReadFiles_whenCreatingNewOne() throws IOException {
    // Files that cannot be read from are considered fully expired.
    File expiredReadableFile = new File(rootDir, "1000");
    // Files that cannot be written, but can still be read, aren't ready to be deleted.
    File expiredWritableFile = new File(rootDir, "10000");
    createFiles(expiredReadableFile, expiredWritableFile);
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(11_500L));

    WritableFile file = folderManager.createWritableFile();

    assertThat(expiredReadableFile.exists()).isFalse();
    assertThat(expiredWritableFile.exists()).isTrue();
    assertThat(file.getFile()).isNotEqualTo(expiredWritableFile);
  }

  @Test
  void closeExpiredReadableFileInUseIfAny_whenPurgingExpiredForReadFiles_whenCreatingNewOne()
      throws IOException {
    File expiredReadableFileBeingRead = new File(rootDir, "900");
    File expiredReadableFile = new File(rootDir, "1000");
    File expiredWritableFile = new File(rootDir, "10000");
    createFiles(expiredReadableFile, expiredWritableFile, expiredReadableFileBeingRead);

    when(clock.now()).thenReturn(MILLISECONDS.toNanos(900 + MIN_FILE_AGE_FOR_READ_MILLIS));
    ReadableFile readableFile = getReadableFile();
    assertThat(readableFile.getFile()).isEqualTo(expiredReadableFileBeingRead);

    when(clock.now()).thenReturn(MILLISECONDS.toNanos(11_500L));

    WritableFile file = folderManager.createWritableFile();

    assertThat(expiredReadableFile.exists()).isFalse();
    assertThat(expiredReadableFileBeingRead.exists()).isFalse();
    assertThat(expiredWritableFile.exists()).isTrue();
    assertThat(file.getFile()).isNotEqualTo(expiredWritableFile);
    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void provideFileForRead_afterItsMinFileAgeForReadTimePassed() throws IOException {
    long readableFileCreationTime = 1000;
    long currentTime =
        MILLISECONDS.toNanos(readableFileCreationTime + MIN_FILE_AGE_FOR_READ_MILLIS);
    when(clock.now()).thenReturn(currentTime);
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File readableFile = new File(rootDir, String.valueOf(readableFileCreationTime));
    createFiles(writableFile, readableFile);

    ReadableFile file = getReadableFile();

    assertThat(file.getFile()).isEqualTo(readableFile);
  }

  @Test
  void provideOldestFileForRead_whenMultipleReadableFilesAreAvailable() throws IOException {
    long firstReadableFileTimestamp = 900;
    long secondReadableFileTimestamp = 1000;
    long thirdReadableFileTimestamp = 1500;
    long currentTime =
        MILLISECONDS.toNanos(thirdReadableFileTimestamp + MIN_FILE_AGE_FOR_READ_MILLIS);
    when(clock.now()).thenReturn(currentTime);
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File firstReadableFile = new File(rootDir, String.valueOf(firstReadableFileTimestamp));
    File secondReadableFile = new File(rootDir, String.valueOf(secondReadableFileTimestamp));
    File thirdReadableFile = new File(rootDir, String.valueOf(thirdReadableFileTimestamp));
    createFiles(writableFile, firstReadableFile, secondReadableFile, thirdReadableFile);

    ReadableFile file = getReadableFile();

    assertThat(file.getFile()).isEqualTo(firstReadableFile);
  }

  @Test
  void provideOldestFileForRead_withCustomFilter() throws IOException {
    long firstReadableFileTimestamp = 900;
    long secondReadableFileTimestamp = 1000;
    long thirdReadableFileTimestamp = 1500;
    long currentTime =
        MILLISECONDS.toNanos(thirdReadableFileTimestamp + MIN_FILE_AGE_FOR_READ_MILLIS);
    when(clock.now()).thenReturn(currentTime);
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File firstReadableFile = new File(rootDir, String.valueOf(firstReadableFileTimestamp));
    File secondReadableFile = new File(rootDir, String.valueOf(secondReadableFileTimestamp));
    File thirdReadableFile = new File(rootDir, String.valueOf(thirdReadableFileTimestamp));
    createFiles(writableFile, firstReadableFile, secondReadableFile, thirdReadableFile);

    ReadableFile file =
        getReadableFile(
            it -> {
              // Exclude the oldest file so that the next oldest is selected.
              return it.createdTimeMillis <= firstReadableFileTimestamp;
            });

    assertThat(file.getFile()).isEqualTo(secondReadableFile);
  }

  @Test
  void provideNullFileForRead_whenNoFilesAreAvailable() throws IOException {
    assertThat(getReadableFile()).isNull();
  }

  @Test
  void provideNullFileForRead_whenOnlyWritableFilesAreAvailable() throws IOException {
    long currentTime = 1000;
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    createFiles(writableFile);

    assertThat(getReadableFile()).isNull();
  }

  @Test
  void provideNullFileForRead_whenReadableFilesAreExpired() throws IOException {
    long creationReferenceTime = 1000;
    File expiredReadableFile1 = new File(rootDir, String.valueOf(creationReferenceTime - 1));
    File expiredReadableFile2 = new File(rootDir, String.valueOf(creationReferenceTime - 10));
    createFiles(expiredReadableFile1, expiredReadableFile2);
    when(clock.now()).thenReturn(creationReferenceTime + MAX_FILE_AGE_FOR_READ_MILLIS);

    assertThat(getReadableFile()).isNull();
  }

  private ReadableFile getReadableFile() throws IOException {
    return getReadableFile(file -> false);
  }

  private ReadableFile getReadableFile(Predicate<FolderManager.CacheFile> exclude)
      throws IOException {
    return folderManager.getReadableFile(exclude);
  }

  private static void fillWithBytes(File file, int size) throws IOException {
    Files.write(file.toPath(), new byte[size]);
  }

  private static void createFiles(File... files) throws IOException {
    for (File file : files) {
      if (!file.createNewFile()) {
        fail("Could not create temporary file: " + file);
      }
    }
  }
}
