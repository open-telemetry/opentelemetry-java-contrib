package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadableFileTest {

  @TempDir File dir;
  private File source;
  private ReadableFile readableFile;

  @BeforeEach
  public void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    Files.write(source.toPath(), Arrays.asList("First line", "Second line"));
    readableFile = new ReadableFile(source);
  }

  @Test
  public void readLineAndRemoveIt() throws IOException {
    readableFile.readLine(bytes -> true);
    readableFile.close();

    List<String> sourceLines = getSourceLines();
    assertEquals(1, sourceLines.size());
    assertEquals("Second line", sourceLines.get(0));
  }

  private List<String> getSourceLines() throws IOException {
    return Files.readAllLines(source.toPath());
  }
}
