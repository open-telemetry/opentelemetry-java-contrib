/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_WRITE_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WritableFileTest {

  @TempDir File rootDir;
  private Clock clock;
  private WritableFile writableFile;
  private static final long CREATED_TIME_MILLIS = 1000L;
  private static final byte[] NEW_LINE_BYTES =
      System.lineSeparator().getBytes(StandardCharsets.UTF_8);
  private static final int NEW_LINE_BYTES_SIZE = NEW_LINE_BYTES.length;

  @BeforeEach
  void setUp() throws IOException {
    clock = mock();
    writableFile =
        new WritableFile(
            new File(rootDir, String.valueOf(CREATED_TIME_MILLIS)),
            CREATED_TIME_MILLIS,
            TestData.getDefaultConfiguration(),
            clock);
  }

  @Test
  void hasNotExpired_whenWriteAgeHasNotExpired() {
    doReturn(MILLISECONDS.toNanos(1500L)).when(clock).now();

    assertFalse(writableFile.hasExpired());
  }

  @Test
  void hasExpired_whenWriteAgeHasExpired() {
    doReturn(MILLISECONDS.toNanos(2000L)).when(clock).now();

    assertTrue(writableFile.hasExpired());
  }

  @Test
  void appendDataInNewLines_andIncreaseSize() throws IOException {
    byte[] line1 = getByteArrayLine("First line");
    byte[] line2 = getByteArrayLine("Second line");
    writableFile.append(line1);
    writableFile.append(line2);
    writableFile.close();

    List<String> lines = getWrittenLines();

    assertEquals(2, lines.size());
    assertEquals("First line", lines.get(0));
    assertEquals("Second line", lines.get(1));
    assertEquals(line1.length + line2.length, writableFile.getSize());
  }

  @Test
  void whenAppendingData_andNotEnoughSpaceIsAvailable_closeAndReturnFailed() throws IOException {
    assertEquals(WritableResult.SUCCEEDED, writableFile.append(new byte[MAX_FILE_SIZE]));

    assertEquals(WritableResult.FAILED, writableFile.append(new byte[1]));

    assertEquals(1, getWrittenLines().size());
    assertEquals(MAX_FILE_SIZE, writableFile.getSize());
  }

  @Test
  void whenAppendingData_andHasExpired_closeAndReturnExpiredStatus() throws IOException {
    writableFile.append(new byte[2]);
    doReturn(MILLISECONDS.toNanos(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_WRITE_MILLIS))
        .when(clock)
        .now();

    assertEquals(WritableResult.FAILED, writableFile.append(new byte[1]));

    assertEquals(1, getWrittenLines().size());
  }

  @Test
  void whenAppendingData_andIsAlreadyClosed_returnFailedStatus() throws IOException {
    writableFile.append(new byte[1]);
    writableFile.close();

    assertEquals(WritableResult.FAILED, writableFile.append(new byte[2]));
  }

  private static byte[] getByteArrayLine(String line) {
    byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8);
    byte[] fullLine = new byte[lineBytes.length + NEW_LINE_BYTES_SIZE];
    System.arraycopy(lineBytes, 0, fullLine, 0, lineBytes.length);
    System.arraycopy(NEW_LINE_BYTES, 0, fullLine, lineBytes.length, NEW_LINE_BYTES_SIZE);
    return fullLine;
  }

  private List<String> getWrittenLines() throws IOException {
    return Files.readAllLines(writableFile.getFile().toPath());
  }
}
