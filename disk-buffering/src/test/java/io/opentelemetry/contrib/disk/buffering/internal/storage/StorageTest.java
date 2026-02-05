/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.FIRST_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_WRITE_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.SECOND_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.THIRD_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.getConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageTest {
  @TempDir private File destinationDir;
  private FolderManager folderManager;
  private Storage<LogRecordData> storage;
  private SignalSerializer<LogRecordData> serializer;
  private AtomicLong currentTimeMillis;
  private static final SignalDeserializer<LogRecordData> DESERIALIZER = SignalDeserializer.ofLogs();

  @BeforeEach
  void setUp() {
    currentTimeMillis = new AtomicLong(0);
    serializer = SignalSerializer.ofLogs();
    folderManager = FolderManager.create(destinationDir, getConfiguration(), new TestClock());
    storage = new Storage<>(folderManager);
  }

  @AfterEach
  void tearDown() throws IOException {
    storage.close();
  }

  @Test
  void writeAndRead() throws IOException {
    assertThat(write(Arrays.asList(FIRST_LOG_RECORD, SECOND_LOG_RECORD))).isTrue();
    assertThat(write(Collections.singletonList(THIRD_LOG_RECORD))).isTrue();
    assertThat(destinationDir.list()).hasSize(1);
    forwardToReadTime();

    ReadableResult<LogRecordData> readResult = storage.readNext(DESERIALIZER);
    assertThat(readResult).isNotNull();
    assertThat(readResult.getContent()).containsExactly(FIRST_LOG_RECORD, SECOND_LOG_RECORD);
    assertThat(destinationDir.list()).hasSize(1);

    // Delete result and read again
    readResult.delete();
    readResult.close();
    ReadableResult<LogRecordData> readResult2 = storage.readNext(DESERIALIZER);
    assertThat(readResult2).isNotNull();
    assertThat(readResult2.getContent()).containsExactly(THIRD_LOG_RECORD);
    assertThat(destinationDir.list()).hasSize(1);

    // Read again without closing previous result
    try {
      storage.readNext(DESERIALIZER);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e)
          .hasMessage("You must close any previous ReadableResult before requesting a new one");
    }

    // Read again when no more data is available
    readResult2.close();
    assertThat(storage.readNext(DESERIALIZER)).isNull();
    assertThat(destinationDir.list()).hasSize(1);
  }

  @Test
  void interactionAfterClosed() throws IOException {
    assertThat(write(Arrays.asList(FIRST_LOG_RECORD, SECOND_LOG_RECORD))).isTrue();
    storage.close();
    assertThat(destinationDir.list()).hasSize(1);
    forwardToReadTime();

    // Reading
    assertThat(storage.readNext(DESERIALIZER)).isNull();

    // Writing
    assertThat(write(Collections.singletonList(THIRD_LOG_RECORD))).isFalse();
  }

  @Test
  void whenTheReadTimeExpires_lookForNewFileToRead() throws IOException {
    long firstFileWriteTime = 1000;
    long secondFileWriteTime = firstFileWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;
    currentTimeMillis.set(firstFileWriteTime);
    assertThat(write(Arrays.asList(FIRST_LOG_RECORD, SECOND_LOG_RECORD))).isTrue();

    // Forward past first file write time
    currentTimeMillis.set(secondFileWriteTime);
    assertThat(write(Collections.singletonList(THIRD_LOG_RECORD))).isTrue();
    assertThat(destinationDir.list())
        .containsExactlyInAnyOrder(
            String.valueOf(firstFileWriteTime), String.valueOf(secondFileWriteTime));

    // Forward past first time read
    currentTimeMillis.set(firstFileWriteTime + MAX_FILE_AGE_FOR_READ_MILLIS + 1);

    // Read
    ReadableResult<LogRecordData> result = storage.readNext(DESERIALIZER);
    assertThat(result).isNotNull();
    assertThat(result.getContent()).containsExactly(THIRD_LOG_RECORD);
    assertThat(destinationDir.list())
        .containsExactlyInAnyOrder(
            String.valueOf(firstFileWriteTime), String.valueOf(secondFileWriteTime));

    // Purge expired files on write
    currentTimeMillis.set(50000);
    assertThat(write(Collections.singletonList(FIRST_LOG_RECORD))).isTrue();
    assertThat(destinationDir.list()).containsExactly("50000");
  }

  @Test
  void whenNoMoreLinesToRead_lookForNewFileToRead() throws IOException {
    long firstFileWriteTime = 1000;
    long secondFileWriteTime = firstFileWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;
    currentTimeMillis.set(firstFileWriteTime);
    assertThat(write(Arrays.asList(FIRST_LOG_RECORD, SECOND_LOG_RECORD))).isTrue();

    // Forward past first file write time
    currentTimeMillis.set(secondFileWriteTime);
    assertThat(write(Collections.singletonList(THIRD_LOG_RECORD))).isTrue();
    assertThat(destinationDir.list())
        .containsExactlyInAnyOrder(
            String.valueOf(firstFileWriteTime), String.valueOf(secondFileWriteTime));

    // Forward to all files read time
    currentTimeMillis.set(secondFileWriteTime + MIN_FILE_AGE_FOR_READ_MILLIS);

    // Read
    ReadableResult<LogRecordData> result = storage.readNext(DESERIALIZER);
    assertThat(result).isNotNull();
    assertThat(result.getContent()).containsExactly(FIRST_LOG_RECORD, SECOND_LOG_RECORD);
    assertThat(destinationDir.list())
        .containsExactlyInAnyOrder(
            String.valueOf(firstFileWriteTime), String.valueOf(secondFileWriteTime));
    result.delete();
    result.close();

    // Read again
    ReadableResult<LogRecordData> result2 = storage.readNext(DESERIALIZER);
    assertThat(result2).isNotNull();
    assertThat(result2.getContent()).containsExactly(THIRD_LOG_RECORD);
    assertThat(destinationDir.list()).containsExactly(String.valueOf(secondFileWriteTime));
    result2.close();
  }

  @Test
  void deleteFilesWithCorruptedData() throws IOException {
    // Add files with invalid data
    Files.write(
        new File(destinationDir, "1000").toPath(), "random data".getBytes(StandardCharsets.UTF_8));
    Files.write(
        new File(destinationDir, "2000").toPath(), "random data".getBytes(StandardCharsets.UTF_8));
    Files.write(
        new File(destinationDir, "3000").toPath(), "random data".getBytes(StandardCharsets.UTF_8));
    Files.write(
        new File(destinationDir, "4000").toPath(), "random data".getBytes(StandardCharsets.UTF_8));

    // Set time ready to read all files
    currentTimeMillis.set(4000 + MIN_FILE_AGE_FOR_READ_MILLIS);

    // Read
    assertThat(storage.readNext(DESERIALIZER)).isNull();
    assertThat(destinationDir.list()).containsExactly("4000"); // it tries 3 times max per call.
  }

  private void forwardToReadTime() {
    forwardCurrentTimeByMillis(MIN_FILE_AGE_FOR_READ_MILLIS);
  }

  private void forwardCurrentTimeByMillis(long millis) {
    currentTimeMillis.set(currentTimeMillis.get() + millis);
  }

  private boolean write(Collection<LogRecordData> items) throws IOException {
    serializer.initialize(items);
    try {
      return storage.write(serializer);
    } finally {
      serializer.reset();
    }
  }

  private class TestClock implements Clock {

    @Override
    public long now() {
      return TimeUnit.MILLISECONDS.toNanos(currentTimeMillis.get());
    }

    @Override
    public long nanoTime() {
      return 0;
    }
  }
}
