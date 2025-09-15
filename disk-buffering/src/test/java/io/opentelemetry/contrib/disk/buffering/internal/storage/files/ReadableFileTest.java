/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.getConfiguration;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ProcessResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
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
  private static final LogRecordData FIRST_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("First log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  private static final LogRecordData SECOND_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Second log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("event")
          .build();

  private static final LogRecordData THIRD_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Third log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  @BeforeEach
  void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    addFileContents(source);
    clock = mock();
    readableFile = new ReadableFile(source, CREATED_TIME_MILLIS, clock, getConfiguration(dir));
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
  void readSingleItemAndRemoveIt() throws IOException {
    readableFile.readAndProcess(
        bytes -> {
          assertThat(deserialize(bytes)).isEqualTo(FIRST_LOG_RECORD);
          return ProcessResult.SUCCEEDED;
        });

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertThat(logs.size()).isEqualTo(2);
    assertThat(logs.get(0)).isEqualTo(SECOND_LOG_RECORD);
    assertThat(logs.get(1)).isEqualTo(THIRD_LOG_RECORD);
  }

  @Test
  void whenProcessingSucceeds_returnSuccessStatus() throws IOException {
    assertThat(readableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED))
        .isEqualTo(ReadableResult.SUCCEEDED);
  }

  @Test
  void whenProcessingFails_returnTryLaterStatus() throws IOException {
    assertThat(readableFile.readAndProcess(bytes -> ProcessResult.TRY_LATER))
        .isEqualTo(ReadableResult.TRY_LATER);
  }

  @Test
  void readMultipleLinesAndRemoveThem() throws IOException {
    readableFile.readAndProcess(
        bytes -> {
          assertDeserializedData(FIRST_LOG_RECORD, bytes);
          return ProcessResult.SUCCEEDED;
        });
    readableFile.readAndProcess(
        bytes -> {
          assertDeserializedData(SECOND_LOG_RECORD, bytes);
          return ProcessResult.SUCCEEDED;
        });

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertThat(logs.size()).isEqualTo(1);
    assertThat(logs.get(0)).isEqualTo(THIRD_LOG_RECORD);
  }

  @Test
  void whenConsumerReturnsFalse_doNotRemoveLineFromSource() throws IOException {
    readableFile.readAndProcess(bytes -> ProcessResult.TRY_LATER);

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertThat(logs.size()).isEqualTo(3);
  }

  @Test
  void whenReadingLastLine_deleteOriginalFile_and_close() throws IOException {
    getRemainingDataAndClose(readableFile);

    assertThat(source.exists()).isFalse();
    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void whenTheFileContentIsInvalid_deleteOriginalFile_and_close() throws IOException {
    assertThat(readableFile.readAndProcess(bytes -> ProcessResult.CONTENT_INVALID))
        .isEqualTo(ReadableResult.FAILED);

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
        new ReadableFile(emptyFile, CREATED_TIME_MILLIS, clock, getConfiguration(dir));

    assertThat(emptyReadableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED))
        .isEqualTo(ReadableResult.FAILED);

    assertThat(emptyReadableFile.isClosed()).isTrue();
    assertThat(emptyFile.exists()).isFalse();
  }

  @Test
  void
      whenReadingAfterTheConfiguredReadingTimeExpired_deleteOriginalFile_close_and_returnFileExpiredException()
          throws IOException {
    readableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED);
    when(clock.now())
        .thenReturn(MILLISECONDS.toNanos(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_READ_MILLIS));

    assertThat(readableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED))
        .isEqualTo(ReadableResult.FAILED);

    assertThat(readableFile.isClosed()).isTrue();
  }

  @Test
  void whenReadingAfterClosed_returnFailedStatus() throws IOException {
    readableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED);
    readableFile.close();

    assertThat(readableFile.readAndProcess(bytes -> ProcessResult.SUCCEEDED))
        .isEqualTo(ReadableResult.FAILED);
  }

  private static void assertDeserializedData(LogRecordData expected, byte[] bytes) {
    try {
      List<LogRecordData> deserialized = DESERIALIZER.deserialize(bytes);
      assertThat(deserialized.get(0)).isEqualTo(expected);
    } catch (DeserializationException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<LogRecordData> getRemainingDataAndClose(ReadableFile readableFile)
      throws IOException {
    List<LogRecordData> result = new ArrayList<>();
    ReadableResult readableResult = ReadableResult.SUCCEEDED;
    while (readableResult == ReadableResult.SUCCEEDED) {
      readableResult =
          readableFile.readAndProcess(
              bytes -> {
                result.add(deserialize(bytes));
                return ProcessResult.SUCCEEDED;
              });
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
