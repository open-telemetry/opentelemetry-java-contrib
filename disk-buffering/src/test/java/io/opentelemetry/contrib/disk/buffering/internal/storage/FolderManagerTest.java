package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.StorageFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderManagerTest {

  @TempDir File rootDir;
  private FolderManager folderManager;
  private TimeProvider timeProvider;

  @BeforeEach
  public void setUp() {
    timeProvider = mock();
    folderManager = new FolderManager(rootDir, timeProvider, TestData.CONFIGURATION);
  }

  @Test
  public void createWritableFile_withTimeMillisAsName() throws IOException {
    doReturn(1000L).when(timeProvider).getSystemCurrentTimeMillis();

    StorageFile file = folderManager.createWritableFile();

    assertEquals("1000", file.file.getName());
  }

  @Test
  public void createWritableFile_andRemoveOldestOne_whenTheAvailableFolderSpaceIsNotEnough()
      throws IOException {
    File existingFile1 = new File(rootDir, "1000");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "1100");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    StorageFile file = folderManager.createWritableFile();

    assertNotEquals(existingFile1, file.file);
    assertNotEquals(existingFile2, file.file);
    assertNotEquals(existingFile3, file.file);
    assertTrue(existingFile2.exists());
    assertTrue(existingFile3.exists());
    assertFalse(existingFile1.exists());
  }

  @Test
  public void
      closeCurrentlyReadableFileIfAny_whenItIsTheOldestOne_andRemoveIt_whenTheAvailableFolderSpaceIsNotEnough()
          throws IOException {
    File existingFile1 = new File(rootDir, "1000");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "1100");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    doReturn(1000L + MIN_FILE_AGE_FOR_READ_MILLIS).when(timeProvider).getSystemCurrentTimeMillis();

    ReadableFile readableFile = folderManager.getReadableFile();
    assertEquals(existingFile1, readableFile.file);

    folderManager.createWritableFile();

    assertTrue(existingFile2.exists());
    assertTrue(existingFile3.exists());
    assertFalse(existingFile1.exists());
    assertTrue(readableFile.isClosed());
  }

  @Test
  public void createWritableFile_andDoNotRemoveOldestOne_ifAtLeastOneExpiredFileIsPurged()
      throws IOException {
    File existingFile1 = new File(rootDir, "1100");
    File existingFile2 = new File(rootDir, "1400");
    File existingFile3 = new File(rootDir, "900");
    createFiles(existingFile3, existingFile2, existingFile1);
    fillWithBytes(existingFile1, MAX_FILE_SIZE);
    fillWithBytes(existingFile2, MAX_FILE_SIZE);
    fillWithBytes(existingFile3, MAX_FILE_SIZE);
    doReturn(11_000L).when(timeProvider).getSystemCurrentTimeMillis();

    StorageFile file = folderManager.createWritableFile();

    assertNotEquals(existingFile1, file.file);
    assertNotEquals(existingFile2, file.file);
    assertNotEquals(existingFile3, file.file);
    assertTrue(existingFile2.exists());
    assertTrue(existingFile1.exists());
    assertFalse(existingFile3.exists());
  }

  @Test
  public void purgeExpiredForReadFiles_whenCreatingNewOne() throws IOException {
    // Files that cannot be read from are considered fully expired.
    File expiredReadableFile = new File(rootDir, "1000");
    // Files that cannot be written, but can still be read, aren't ready to be deleted.
    File expiredWritableFile = new File(rootDir, "10000");
    createFiles(expiredReadableFile, expiredWritableFile);
    doReturn(11_500L).when(timeProvider).getSystemCurrentTimeMillis();

    StorageFile file = folderManager.createWritableFile();

    assertFalse(expiredReadableFile.exists());
    assertTrue(expiredWritableFile.exists());
    assertNotEquals(expiredWritableFile, file.file);
  }

  @Test
  public void closeExpiredReadableFileInUseIfAny_whenPurgingExpiredForReadFiles_whenCreatingNewOne()
      throws IOException {
    File expiredReadableFileBeingRead = new File(rootDir, "900");
    File expiredReadableFile = new File(rootDir, "1000");
    File expiredWritableFile = new File(rootDir, "10000");
    createFiles(expiredReadableFile, expiredWritableFile, expiredReadableFileBeingRead);

    doReturn(900 + MIN_FILE_AGE_FOR_READ_MILLIS).when(timeProvider).getSystemCurrentTimeMillis();
    ReadableFile readableFile = folderManager.getReadableFile();
    assertEquals(expiredReadableFileBeingRead, readableFile.file);

    doReturn(11_500L).when(timeProvider).getSystemCurrentTimeMillis();

    StorageFile file = folderManager.createWritableFile();

    assertFalse(expiredReadableFile.exists());
    assertFalse(expiredReadableFileBeingRead.exists());
    assertTrue(expiredWritableFile.exists());
    assertNotEquals(expiredWritableFile, file.file);
    assertTrue(readableFile.isClosed());
  }

  @Test
  public void provideFileForRead_afterItsMinFileAgeForReadTimePassed() throws IOException {
    long readableFileCreationTime = 1000;
    long currentTime = readableFileCreationTime + MIN_FILE_AGE_FOR_READ_MILLIS;
    doReturn(currentTime).when(timeProvider).getSystemCurrentTimeMillis();
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File readableFile = new File(rootDir, String.valueOf(readableFileCreationTime));
    createFiles(writableFile, readableFile);

    StorageFile file = folderManager.getReadableFile();

    assertEquals(readableFile, file.file);
  }

  @Test
  public void provideOldestFileForRead_whenMultipleReadableFilesAreAvailable() throws IOException {
    long newerReadableFileCreationTime = 1000;
    long olderReadableFileCreationTime = 900;
    long currentTime = newerReadableFileCreationTime + MIN_FILE_AGE_FOR_READ_MILLIS;
    doReturn(currentTime).when(timeProvider).getSystemCurrentTimeMillis();
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    File readableFileOlder = new File(rootDir, String.valueOf(olderReadableFileCreationTime));
    File readableFileNewer = new File(rootDir, String.valueOf(newerReadableFileCreationTime));
    createFiles(writableFile, readableFileNewer, readableFileOlder);

    StorageFile file = folderManager.getReadableFile();

    assertEquals(readableFileOlder, file.file);
  }

  @Test
  public void provideNullFileForRead_whenNoFilesAreAvailable() throws IOException {
    assertNull(folderManager.getReadableFile());
  }

  @Test
  public void provideNullFileForRead_whenOnlyReadableFilesAreAvailable() throws IOException {
    long currentTime = 1000;
    File writableFile = new File(rootDir, String.valueOf(currentTime));
    createFiles(writableFile);

    assertNull(folderManager.getReadableFile());
  }

  @Test
  public void provideNullFileForRead_whenReadableFilesAreExpired() throws IOException {
    long creationReferenceTime = 1000;
    File expiredReadableFile1 = new File(rootDir, String.valueOf(creationReferenceTime - 1));
    File expiredReadableFile2 = new File(rootDir, String.valueOf(creationReferenceTime - 10));
    createFiles(expiredReadableFile1, expiredReadableFile2);
    doReturn(creationReferenceTime + MAX_FILE_AGE_FOR_READ_MILLIS)
        .when(timeProvider)
        .getSystemCurrentTimeMillis();

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
