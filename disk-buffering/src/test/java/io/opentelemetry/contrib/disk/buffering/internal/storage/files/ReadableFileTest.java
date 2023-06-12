package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.CONFIGURATION;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoMoreLinesToReadException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.TemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadableFileTest {

  @TempDir File dir;
  private File source;
  private File temporaryFile;
  private ReadableFile readableFile;
  private TimeProvider timeProvider;
  private TemporaryFileProvider temporaryFileProvider;
  private static final List<String> LINES =
      Arrays.asList("First line", "Second line", "Third line");
  private static final long CREATED_TIME_MILLIS = 1000L;

  @BeforeEach
  public void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    temporaryFile = new File(dir, "temporaryFile");
    Files.write(source.toPath(), LINES);
    temporaryFileProvider = mock();
    doReturn(temporaryFile).when(temporaryFileProvider).createTemporaryFile(anyString());
    timeProvider = mock();
    readableFile =
        new ReadableFile(
            source, CREATED_TIME_MILLIS, timeProvider, CONFIGURATION, temporaryFileProvider);
  }

  @Test
  public void readSingleLineAndRemoveIt() throws IOException {
    readableFile.readLine(
        bytes -> {
          String lineRead = new String(bytes, StandardCharsets.UTF_8);
          assertEquals("First line", lineRead);
          return true;
        });
    readableFile.close();

    List<String> sourceLines = getSourceLines();
    assertEquals(2, sourceLines.size());
    assertEquals("Second line", sourceLines.get(0));
    assertEquals("Third line", sourceLines.get(1));
  }

  @Test
  public void deleteTemporaryFileWhenClosing() throws IOException {
    readableFile.readLine(
        bytes -> {
          String lineRead = new String(bytes, StandardCharsets.UTF_8);
          assertEquals("First line", lineRead);
          return true;
        });
    readableFile.close();

    assertFalse(temporaryFile.exists());
  }

  @Test
  public void readMultipleLinesAndRemoveThem() throws IOException {
    readableFile.readLine(
        bytes -> {
          String lineRead = new String(bytes, StandardCharsets.UTF_8);
          assertEquals("First line", lineRead);
          return true;
        });
    readableFile.readLine(
        bytes -> {
          String lineRead = new String(bytes, StandardCharsets.UTF_8);
          assertEquals("Second line", lineRead);
          return true;
        });
    readableFile.close();

    List<String> sourceLines = getSourceLines();
    assertEquals(1, sourceLines.size());
    assertEquals("Third line", sourceLines.get(0));
  }

  @Test
  public void whenConsumerReturnsFalse_doNotRemoveLineFromSource() throws IOException {
    readableFile.readLine(
        bytes -> {
          String lineRead = new String(bytes, StandardCharsets.UTF_8);
          assertEquals("First line", lineRead);
          return false;
        });
    readableFile.close();

    List<String> sourceLines = getSourceLines();
    assertEquals(3, sourceLines.size());
    assertEquals(LINES, sourceLines);
  }

  @Test
  public void whenReadingLastLine_deleteOriginalFile_and_close() throws IOException {
    for (String line : LINES) {
      readableFile.readLine(
          bytes -> {
            assertEquals(line, new String(bytes, StandardCharsets.UTF_8));
            return true;
          });
    }

    assertFalse(source.exists());
    assertTrue(readableFile.isClosed());
  }

  @Test
  public void whenNoMoreLinesAvailableToRead_deleteOriginalFile_close_and_throwException()
      throws IOException {
    File emptyFile = new File(dir, "emptyFile");
    try (FileOutputStream ignored = new FileOutputStream(emptyFile)) {}

    ReadableFile emptyReadableFile =
        new ReadableFile(
            emptyFile, CREATED_TIME_MILLIS, timeProvider, CONFIGURATION, temporaryFileProvider);
    try {
      emptyReadableFile.readLine(bytes -> true);
      fail();
    } catch (NoMoreLinesToReadException ignored) {
      assertTrue(emptyReadableFile.isClosed());
      assertFalse(emptyFile.exists());
    }
  }

  @Test
  public void whenReadingAfterTheConfiguredReadingTimeExpired_throwException() throws IOException {
    readableFile.readLine(bytes -> true);
    doReturn(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_READ_MILLIS)
        .when(timeProvider)
        .getSystemCurrentTimeMillis();

    try {
      readableFile.readLine(bytes -> true);
      fail();
    } catch (ReadingTimeoutException ignored) {
    }
  }

  @Test
  public void whenReadingAfterClosed_throwException() throws IOException {
    readableFile.readLine(bytes -> true);
    readableFile.close();

    try {
      readableFile.readLine(bytes -> true);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  private List<String> getSourceLines() throws IOException {
    return Files.readAllLines(source.toPath());
  }
}
