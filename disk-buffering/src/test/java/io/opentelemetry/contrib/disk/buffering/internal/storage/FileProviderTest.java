package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileProviderTest {

  @TempDir File rootDir;
  private FileProvider fileProvider;
  private TimeProvider timeProvider;
  private static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  private static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  private static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;

  @BeforeEach
  public void setUp() {
    timeProvider = mock();
    fileProvider =
        new FileProvider(
            rootDir,
            timeProvider,
            new Configuration(
                MAX_FILE_AGE_FOR_WRITE_MILLIS,
                MIN_FILE_AGE_FOR_READ_MILLIS,
                MAX_FILE_AGE_FOR_READ_MILLIS));
  }

  @Test
  public void createWritableFile_withTimeMillisAsName() {
    doReturn(1000L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertEquals("1000", file.getFile().getName());
  }

  @Test
  public void reuseWritableFile_whenItHasNotExpired() throws IOException {
    File existingFile = new File(rootDir, "1000");
    if (!existingFile.createNewFile()) {
      fail("Could not create temporary file");
    }
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertEquals(existingFile, file.getFile());
  }

  @Test
  public void createWritableFile_ifExistingOnesAlreadyExpired() throws IOException {
    File existingFile = new File(rootDir, "1000");
    if (!existingFile.createNewFile()) {
      fail("Could not create temporary file");
    }
    doReturn(2500L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertNotEquals(existingFile, file.getFile());
  }

  @Test
  public void purgeExpiredForReadFiles_whenCreatingNewOne() throws IOException {
    // Files that cannot be read from are considered fully expired.
    File expiredReadableFile = new File(rootDir, "1000");
    // Files that cannot be written, but can still be read, aren't expired.
    File expiredWritableFile = new File(rootDir, "10000");
    if (!expiredReadableFile.createNewFile() || !expiredWritableFile.createNewFile()) {
      fail("Could not create temporary files");
    }
    doReturn(11_500L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertFalse(expiredReadableFile.exists());
    assertTrue(expiredWritableFile.exists());
    assertNotEquals(expiredWritableFile, file.getFile());
  }
}
