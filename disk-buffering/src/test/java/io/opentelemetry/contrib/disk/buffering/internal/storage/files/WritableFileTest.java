package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WritableFileTest {

  @TempDir File rootDir;
  private TimeProvider timeProvider;
  private WritableFile writableFile;
  private static final long CREATED_TIME_MILLIS = 1000L;

  @BeforeEach
  public void setUp() {
    timeProvider = mock();
    writableFile =
        new WritableFile(
            new File(rootDir, String.valueOf(CREATED_TIME_MILLIS)),
            TestData.CONFIGURATION,
            timeProvider);
  }

  @Test
  public void isValid_whenWriteAgeHasNotExpired() {
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    assertTrue(writableFile.isValid());
  }

  @Test
  public void isNotValid_whenWriteAgeHasExpired() {
    doReturn(2000L).when(timeProvider).getSystemCurrentTimeMillis();

    assertFalse(writableFile.isValid());
  }

  @Test
  public void isNotValid_whenSizeHasReachedTheMaxLimit() {
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    writableFile.append(new byte[MAX_FILE_SIZE]);

    assertFalse(writableFile.isValid());
  }
}
