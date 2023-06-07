package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public void verifyWritableFileIsCreated_withTimeMillisAsName() {
    doReturn(1000L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertEquals("1000", file.getFile().getName());
  }

  @Test
  public void verifyWritableFileIsReused_whenItHasNotExpired() throws IOException {
    File existingFile = new File(rootDir, "1000");
    if (!existingFile.createNewFile()) {
      fail("Could not create temporary file");
    }
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    FileHolder file = fileProvider.getWritableFile();

    assertEquals(existingFile, file.getFile());
  }
}
