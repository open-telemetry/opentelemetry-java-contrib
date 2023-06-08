package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoMoreLinesToReadException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.TemporaryFileProvider;
import java.io.File;
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
  private static final List<String> LINES =
      Arrays.asList("First line", "Second line", "Third line");

  @BeforeEach
  public void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    temporaryFile = new File(dir, "temporaryFile");
    Files.write(source.toPath(), LINES);
    TemporaryFileProvider temporaryFileProvider = mock();
    doReturn(temporaryFile).when(temporaryFileProvider).createTemporaryFile();
    readableFile = new ReadableFile(source, temporaryFileProvider);
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
  public void whenNoMoreLinesAvailableToRead_throwException() throws IOException {
    for (String line : LINES) {
      readableFile.readLine(
          bytes -> {
            assertEquals(line, new String(bytes, StandardCharsets.UTF_8));
            return true;
          });
    }

    try {
      readableFile.readLine(bytes -> true);
      fail();
    } catch (NoMoreLinesToReadException ignored) {
    }
  }

  private List<String> getSourceLines() throws IOException {
    return Files.readAllLines(source.toPath());
  }
}
