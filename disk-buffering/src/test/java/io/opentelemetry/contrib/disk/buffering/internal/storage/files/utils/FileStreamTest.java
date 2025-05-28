package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStreamTest {
  @TempDir File dir;

  @Test
  void truncateTop() throws IOException {
    String initialText = "1,2,3,4,5";
    byte[] readBuffer;
    File temporaryFile = new File(dir, "temporaryFile");
    writeString(temporaryFile, initialText);

    FileStream stream = FileStream.create(temporaryFile);

    assertThat((char) stream.read()).asString().isEqualTo("1");
    assertThat(readString(temporaryFile)).isEqualTo(initialText);

    // Truncate until current position
    stream.truncateTop();
    assertThat(readString(temporaryFile)).isEqualTo(",2,3,4,5");

    // Truncate fixed size from the top
    stream.truncateTop(3);

    // Ensure that the changes are made before closing the stream.
    assertThat(readString(temporaryFile)).isEqualTo("3,4,5");

    // Truncate again
    readBuffer = new byte[3];
    stream.read(readBuffer);
    assertThat(readBuffer).asString().isEqualTo("3,4");
    stream.truncateTop(2);

    // Ensure that the changes are made before closing the stream.
    assertThat(readString(temporaryFile)).isEqualTo("4,5");

    stream.close();

    // Ensure that the changes are kept after closing the stream.
    assertThat(readString(temporaryFile)).isEqualTo("4,5");
  }

  private static void writeString(File file, String text) throws IOException {
    Files.write(
        file.toPath(), text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
  }

  @NotNull
  private static String readString(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }
}
