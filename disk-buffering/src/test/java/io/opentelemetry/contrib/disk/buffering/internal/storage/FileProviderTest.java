package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
  private static final long MAX_FILE_AGE_MILLIS = 1000;

  @BeforeEach
  public void setUp() {
    timeProvider = mock();
    fileProvider = new FileProvider(rootDir, timeProvider, new Configuration(MAX_FILE_AGE_MILLIS));
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
  public void purgeExpiredFiles_whenCreatingNewOne() throws IOException {
    File expiredFile1 = new File(rootDir, "1000");
    File expiredFile2 = new File(rootDir, "1100");
    if (!expiredFile1.createNewFile() || !expiredFile2.createNewFile()) {
      fail("Could not create temporary files");
    }
    doReturn(2500L).when(timeProvider).getSystemCurrentTimeMillis();

    fileProvider.getWritableFile();

    assertFalse(expiredFile1.exists());
    assertFalse(expiredFile2.exists());
  }
}
