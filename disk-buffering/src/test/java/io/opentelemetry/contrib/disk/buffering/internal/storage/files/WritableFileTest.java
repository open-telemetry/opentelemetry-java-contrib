/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_WRITE_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_SIZE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.ByteArraySerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
            TestData.getConfiguration(rootDir),
            clock);
  }

  @AfterEach
  void tearDown() throws IOException {
    writableFile.close();
  }

  @Test
  void hasNotExpired_whenWriteAgeHasNotExpired() {
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(1500L));

    assertThat(writableFile.hasExpired()).isFalse();
  }

  @Test
  void hasExpired_whenWriteAgeHasExpired() {
    when(clock.now()).thenReturn(MILLISECONDS.toNanos(2000L));

    assertThat(writableFile.hasExpired()).isTrue();
  }

  @Test
  void appendDataInNewLines_andIncreaseSize() throws IOException {
    byte[] line1 = getByteArrayLine("First line");
    byte[] line2 = getByteArrayLine("Second line");
    writableFile.append(new ByteArraySerializer(line1));
    writableFile.append(new ByteArraySerializer(line2));
    writableFile.close();

    List<String> lines = getWrittenLines();

    assertThat(lines).hasSize(2);
    assertThat(lines.get(0)).isEqualTo("First line");
    assertThat(lines.get(1)).isEqualTo("Second line");
    assertThat(writableFile.getSize()).isEqualTo(line1.length + line2.length);
  }

  @Test
  void whenAppendingData_andNotEnoughSpaceIsAvailable_closeAndReturnFailed() throws IOException {
    assertThat(writableFile.append(new ByteArraySerializer(new byte[MAX_FILE_SIZE])))
        .isEqualTo(WritableResult.SUCCEEDED);

    assertThat(writableFile.append(new ByteArraySerializer(new byte[1])))
        .isEqualTo(WritableResult.FAILED);

    assertThat(getWrittenLines()).hasSize(1);
    assertThat(writableFile.getSize()).isEqualTo(MAX_FILE_SIZE);
  }

  @Test
  void whenAppendingData_andHasExpired_closeAndReturnExpiredStatus() throws IOException {
    writableFile.append(new ByteArraySerializer(new byte[2]));
    when(clock.now())
        .thenReturn(MILLISECONDS.toNanos(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_WRITE_MILLIS));

    assertThat(writableFile.append(new ByteArraySerializer(new byte[1])))
        .isEqualTo(WritableResult.FAILED);

    assertThat(getWrittenLines()).hasSize(1);
  }

  @Test
  void whenAppendingData_andIsAlreadyClosed_returnFailedStatus() throws IOException {
    writableFile.append(new ByteArraySerializer(new byte[1]));
    writableFile.close();

    assertThat(writableFile.append(new ByteArraySerializer(new byte[2])))
        .isEqualTo(WritableResult.FAILED);
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
