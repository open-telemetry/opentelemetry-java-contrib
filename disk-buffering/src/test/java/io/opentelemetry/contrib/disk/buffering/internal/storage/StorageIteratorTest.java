/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.FIRST_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_WRITE_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.SECOND_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.THIRD_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.getConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageIteratorTest {
  @TempDir private File destinationDir;
  private Storage<LogRecordData> storage;
  private SignalSerializer<LogRecordData> serializer;
  private AtomicLong currentTimeMillis;
  private static final SignalDeserializer<LogRecordData> DESERIALIZER = SignalDeserializer.ofLogs();

  @BeforeEach
  void setUp() {
    currentTimeMillis = new AtomicLong(0);
    serializer = SignalSerializer.ofLogs();
    FolderManager folderManager =
        FolderManager.create(destinationDir, getConfiguration(), new TestClock());
    storage = new Storage<>(folderManager);
  }

  @AfterEach
  void tearDown() throws IOException {
    storage.close();
  }

  @Test
  void removeBeforeNext_throwsIllegalStateException() throws IOException {
    writeItem(FIRST_LOG_RECORD);
    forwardToReadTime();

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    assertThat(iterator.hasNext()).isTrue();

    assertThatThrownBy(iterator::remove)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("next() must be called before remove()");
  }

  @Test
  void doubleRemove_throwsIllegalStateException() throws IOException {
    writeItem(FIRST_LOG_RECORD);
    forwardToReadTime();

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    iterator.next();
    iterator.remove();

    assertThatThrownBy(iterator::remove)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("next() must be called before remove()");
  }

  @Test
  void iterateWithRemove_deletesFiles() throws IOException {
    writeItem(FIRST_LOG_RECORD);
    forwardToReadTime();

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    List<LogRecordData> items = new ArrayList<>();
    while (iterator.hasNext()) {
      items.addAll(iterator.next());
      iterator.remove();
    }

    assertThat(items).containsExactly(FIRST_LOG_RECORD);
    assertThat(destinationDir.list()).isEmpty();
  }

  @Test
  void iterateWithoutRemove_preservesFiles() throws IOException {
    writeItem(FIRST_LOG_RECORD);
    forwardToReadTime();

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    List<LogRecordData> items = new ArrayList<>();
    while (iterator.hasNext()) {
      items.addAll(iterator.next());
    }

    assertThat(items).containsExactly(FIRST_LOG_RECORD);
    assertThat(destinationDir.list()).hasSize(1);
  }

  @Test
  void iterateAcrossMultipleFiles_withRemove() throws IOException {
    long firstWriteTime = 1000;
    long secondWriteTime = firstWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;

    currentTimeMillis.set(firstWriteTime);
    writeItem(FIRST_LOG_RECORD);

    currentTimeMillis.set(secondWriteTime);
    writeItem(SECOND_LOG_RECORD);

    currentTimeMillis.set(secondWriteTime + MIN_FILE_AGE_FOR_READ_MILLIS);

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    List<LogRecordData> items = new ArrayList<>();
    while (iterator.hasNext()) {
      items.addAll(iterator.next());
      iterator.remove();
    }

    assertThat(items).containsExactly(FIRST_LOG_RECORD, SECOND_LOG_RECORD);
    assertThat(destinationDir.list()).isEmpty();
  }

  @Test
  void iterateAcrossMultipleFiles_withoutRemove() throws IOException {
    long firstWriteTime = 1000;
    long secondWriteTime = firstWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;

    currentTimeMillis.set(firstWriteTime);
    writeItem(FIRST_LOG_RECORD);

    currentTimeMillis.set(secondWriteTime);
    writeItem(SECOND_LOG_RECORD);

    currentTimeMillis.set(secondWriteTime + MIN_FILE_AGE_FOR_READ_MILLIS);

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    List<LogRecordData> items = new ArrayList<>();
    while (iterator.hasNext()) {
      items.addAll(iterator.next());
    }

    assertThat(items).containsExactly(FIRST_LOG_RECORD, SECOND_LOG_RECORD);
    assertThat(destinationDir.list()).hasSize(2);
  }

  @Test
  void selectiveRemove_onlyDeletesRemovedItems() throws IOException {
    long firstWriteTime = 1000;
    long secondWriteTime = firstWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;
    long thirdWriteTime = secondWriteTime + MAX_FILE_AGE_FOR_WRITE_MILLIS + 1;

    currentTimeMillis.set(firstWriteTime);
    writeItem(FIRST_LOG_RECORD);

    currentTimeMillis.set(secondWriteTime);
    writeItem(SECOND_LOG_RECORD);

    currentTimeMillis.set(thirdWriteTime);
    writeItem(THIRD_LOG_RECORD);

    currentTimeMillis.set(thirdWriteTime + MIN_FILE_AGE_FOR_READ_MILLIS);

    Iterator<Collection<LogRecordData>> iterator = new StorageIterator<>(storage, DESERIALIZER);
    List<LogRecordData> items = new ArrayList<>();
    int index = 0;
    while (iterator.hasNext()) {
      items.addAll(iterator.next());
      if (index == 0 || index == 2) {
        iterator.remove();
      }
      index++;
    }

    assertThat(items).containsExactly(FIRST_LOG_RECORD, SECOND_LOG_RECORD, THIRD_LOG_RECORD);
    assertThat(destinationDir.list()).hasSize(1);
    assertThat(destinationDir.list()).containsExactly(String.valueOf(secondWriteTime));
  }

  private void writeItem(LogRecordData item) throws IOException {
    serializer.initialize(Collections.singletonList(item));
    try {
      storage.write(serializer);
    } finally {
      serializer.reset();
    }
  }

  private void forwardToReadTime() {
    currentTimeMillis.set(currentTimeMillis.get() + MIN_FILE_AGE_FOR_READ_MILLIS);
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
