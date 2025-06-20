/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    folderManager = new FolderManager(rootDir, TestData.getConfiguration(rootDir), clock);
  }

  @AfterEach
  void tearDown() throws Exception {
    folderManager.close();
  }

  @Test
  void createWritableFile_withTimeMillisAsName() throws IOException {
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(1000L));
    WritableFile file = folderManager.createWritableFile();

    assertEquals("1000", file.getFile().getName());
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

    assertNotEquals(existingFile1, file.getFile());
    assertNotEquals(existingFile2, file.getFile());
    assertNotEquals(existingFile3, file.getFile());
    assertTrue(existingFile2.exists());
    assertTrue(existingFile3.exists());
    assertFalse(existingFile1.exists());
  }

  @Test
  void closeCurrentlyWritableFile_whenItIsReadyToBeRead_andNoOtherReadableFilesAreAvailable()
      throws IOException {
    long createdFileTime = 1000L;
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(createdFileTime));

    WritableFile writableFile = folderManager.createWritableFile();
    writableFile.append(new byte[3]);

    when(clock.now())
        .thenReturn(MILLISECONDS.toNanos(createdFileTime + MIN_FILE_AGE_FOR_READ_MILLIS));

    ReadableFile readableFile = folderManager.getReadableFile();

    assertEquals(writableFile.getFile(), readableFile.getFile());
    assertTrue(writableFile.isClosed());
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

    ReadableFile readableFile = folderManager.getReadableFile();
    assertEquals(existingFile1, readableFile.getFile());

    folderManager.createWritableFile();

    assertTrue(existingFile2.exists());
    assertTrue(existingFile3.exists());
    assertFalse(existingFile1.exists());
    assertTrue(readableFile.isClosed());
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

    assertNotEquals(existingFile1, file.getFile());
    assertNotEquals(existingFile2, file.getFile());
    assertNotEquals(existingFile3, file.getFile());
    assertTrue(existingFile2.exists());
    assertTrue(existingFile1.exists());
    assertFalse(existingFile3.exists());
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

    assertFalse(expiredReadableFile.exists());
    assertTrue(expiredWritableFile.exists());
    assertNotEquals(expiredWritableFile, file.getFile());
  }

  @Test
  void closeExpiredReadableFileInUseIfAny_whenPurgingExpiredForReadFiles_whenCreatingNewOne()
      throws IOException {
    File expiredReadableFileBeingRead = new File(rootDir, "900");
    File expiredReadableFile = new File(rootDir, "1000");
    File expiredWritableFile = new File(rootDir, "10000");
    createFiles(expiredReadableFile, expiredWritableFile, expiredReadableFileBeingRead);

    when(clock.now()).thenReturn(MILLISECONDS.toNanos(900 + MIN_FILE_AGE_FOR_READ_MILLIS));
    ReadableFile readableFile = folderManager.getReadableFile();
    assertEquals(expiredReadableFileBeingRead, readableFile.getFile());

    when(clock.now()).thenReturn(MILLISECONDS.toNanos(11_500L));

    WritableFile file = folderManager.createWritableFile();

    assertFalse(expiredReadableFile.exists());
    assertFalse(expiredReadableFileBeingRead.exists());
    assertTrue(expiredWritableFile.exists());
    assertNotEquals(expiredWritableFile, file.getFile());
    assertTrue(readableFile.isClosed());
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

    ReadableFile file = folderManager.getReadableFile();

    assertEquals(readableFile, file.getFile());
  }

  @Test
  void provideOldestFileForRead_whenMultipleReadableFilesAreAvailable() throws IOException {
    long newerReadableFileCreationTime = 1000;
    long olderReadableFileCreationTime = 900;
    long currentTime =
        MILLISECONDS.toNanos(newerReadableFileCreationTime + MIN_FILE_AGE_FOR_READ_MILLIS);
    when(clock.now()).thenReturn(currentTime);
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File readableFileOlder = new File(rootDir, String.valueOf(olderReadableFileCreationTime));
    File readableFileNewer = new File(rootDir, String.valueOf(newerReadableFileCreationTime));
    createFiles(writableFile, readableFileNewer, readableFileOlder);

    ReadableFile file = folderManager.getReadableFile();

    assertEquals(readableFileOlder, file.getFile());
  }

  @Test
  void provideNullFileForRead_whenNoFilesAreAvailable() throws IOException {
    assertNull(folderManager.getReadableFile());
  }

  @Test
  void provideNullFileForRead_whenOnlyReadableFilesAreAvailable() throws IOException {
    long currentTime = 1000;
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    createFiles(writableFile);

    assertNull(folderManager.getReadableFile());
  }

  @Test
  void provideNullFileForRead_whenReadableFilesAreExpired() throws IOException {
    long creationReferenceTime = 1000;
    File expiredReadableFile1 = new File(rootDir, String.valueOf(creationReferenceTime - 1));
    File expiredReadableFile2 = new File(rootDir, String.valueOf(creationReferenceTime - 10));
    createFiles(expiredReadableFile1, expiredReadableFile2);
    when(clock.now()).thenReturn(creationReferenceTime + MAX_FILE_AGE_FOR_READ_MILLIS);

    assertNull(folderManager.getReadableFile());
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
