/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.FIRST_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.SECOND_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.THIRD_LOG_RECORD;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.getConfiguration;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadableFileTest {

  @TempDir File dir;
  private File source;
  private ReadableFile readableFile;
  private Clock clock;
  private static final long CREATED_TIME_MILLIS = 1000L;
  private static final SignalSerializer<LogRecordData> SERIALIZER = SignalSerializer.ofLogs();
  private static final SignalDeserializer<LogRecordData> DESERIALIZER = SignalDeserializer.ofLogs();

  @BeforeEach
  void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    addFileContents(source);
    clock = mock();
    readableFile = new ReadableFile(source, CREATED_TIME_MILLIS, clock, getConfiguration());
  }

  @AfterEach
  void tearDown() throws IOException {
    readableFile.close();
  }

  private static void addFileContents(File source) throws IOException {
    try (FileOutputStream out = new FileOutputStream(source)) {
      for (LogRecordData item :
          Arrays.asList(FIRST_LOG_RECORD, SECOND_LOG_RECORD, THIRD_LOG_RECORD)) {
        SERIALIZER.initialize(Collections.singleton(item));
        SERIALIZER.writeBinaryTo(out);
        SERIALIZER.reset();
      }
    }
  }

  @Test
  void readAndRemoveItems() throws IOException {
    assertThat(FIRST_LOG_RECORD).isEqualTo(deserialize(readableFile.readNext()));
    readableFile.removeTopItem();

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertThat(2).isEqualTo(logs.size());
    assertThat(SECOND_LOG_RECORD).isEqualTo(logs.get(0));
    assertThat(THIRD_LOG_RECORD).isEqualTo(logs.get(1));
  }

  @Test
  void whenReadingLastLine_deleteOriginalFile_and_close() throws IOException {
    getRemainingDataAndClose(readableFile);

    assertThat(source.exists()).isFalse();
    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void whenNoMoreLinesAvailableToRead_deleteOriginalFile_close_and_returnNoContentStatus()
      throws IOException {
    File emptyFile = new File(dir, "emptyFile");
    if (!emptyFile.createNewFile()) {
      fail("Could not create file for tests");
    }

    ReadableFile emptyReadableFile =
        new ReadableFile(emptyFile, CREATED_TIME_MILLIS, clock, getConfiguration());

    assertThat(emptyReadableFile.readNext()).isNull();

    assertThat(emptyReadableFile.isClosed()).isTrue();
    assertThat(emptyFile.exists()).isFalse();
  }

  @Test
  void whenReadingAfterTheConfiguredReadingTimeExpired_close() throws IOException {
    when(clock.now())
        .thenReturn(MILLISECONDS.toNanos(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_READ_MILLIS));

    assertThat(readableFile.readNext()).isNull();
    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void whenReadingAfterClosed_returnNull() throws IOException {
    readableFile.close();

    assertThat(readableFile.readNext()).isNull();
  }

  private static List<LogRecordData> getRemainingDataAndClose(ReadableFile readableFile)
      throws IOException {
    List<LogRecordData> result = new ArrayList<>();
    byte[] bytes = readableFile.readNext();
    while (bytes != null) {
      result.add(deserialize(bytes));
      readableFile.removeTopItem();
      bytes = readableFile.readNext();
    }

    readableFile.close();

    return result;
  }

  private static LogRecordData deserialize(byte[] data) {
    try {
      return DESERIALIZER.deserialize(data).get(0);
    } catch (DeserializationException e) {
      throw new RuntimeException(e);
    }
  }
}
