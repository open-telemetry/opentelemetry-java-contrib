/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_WRITE_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants.NEW_LINE_BYTES_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.WritingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
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
  private TimeProvider timeProvider;
  private WritableFile writableFile;
  private static final long CREATED_TIME_MILLIS = 1000L;

  @BeforeEach
  public void setUp() throws IOException {
    timeProvider = mock();
    writableFile =
        new WritableFile(
            new File(rootDir, String.valueOf(CREATED_TIME_MILLIS)),
            CREATED_TIME_MILLIS,
            TestData.CONFIGURATION,
            timeProvider);
  }

  @Test
  public void hasNotExpired_whenWriteAgeHasNotExpired() {
    doReturn(1500L).when(timeProvider).getSystemCurrentTimeMillis();

    assertFalse(writableFile.hasExpired());
  }

  @Test
  public void hasExpired_whenWriteAgeHasExpired() {
    doReturn(2000L).when(timeProvider).getSystemCurrentTimeMillis();

    assertTrue(writableFile.hasExpired());
  }

  @Test
  public void appendDataInNewLines_andIncreaseSize() throws IOException {
    byte[] line1 = "First line".getBytes(StandardCharsets.UTF_8);
    byte[] line2 = "Second line".getBytes(StandardCharsets.UTF_8);
    writableFile.append(line1);
    writableFile.append(line2);
    writableFile.close();

    List<String> lines = getWrittenLines();

    assertEquals(2, lines.size());
    assertEquals("First line", lines.get(0));
    assertEquals("Second line", lines.get(1));
    assertEquals(line1.length + line2.length + NEW_LINE_BYTES_SIZE * 2L, writableFile.getSize());
  }

  @Test
  public void whenAppendingData_andNotEnoughSpaceIsAvailable_closeAndThrowException()
      throws IOException {
    writableFile.append(new byte[MAX_FILE_SIZE - NEW_LINE_BYTES_SIZE]);
    try {
      writableFile.append(new byte[1]);
      fail();
    } catch (NoSpaceAvailableException e) {
      assertEquals(1, getWrittenLines().size());
      assertEquals(MAX_FILE_SIZE, writableFile.getSize());
    }
  }

  @Test
  public void whenAppendingData_andHasExpired_closeAndThrowException() throws IOException {
    writableFile.append(new byte[2]);
    doReturn(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_WRITE_MILLIS)
        .when(timeProvider)
        .getSystemCurrentTimeMillis();
    try {
      writableFile.append(new byte[1]);
      fail();
    } catch (WritingTimeoutException e) {
      assertEquals(1, getWrittenLines().size());
    }
  }

  @Test
  public void whenAppendingData_andIsAlreadyClosed_throwException() throws IOException {
    writableFile.append(new byte[1]);
    writableFile.close();

    try {
      writableFile.append(new byte[2]);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  private List<String> getWrittenLines() throws IOException {
    return Files.readAllLines(writableFile.file.toPath());
  }
}
