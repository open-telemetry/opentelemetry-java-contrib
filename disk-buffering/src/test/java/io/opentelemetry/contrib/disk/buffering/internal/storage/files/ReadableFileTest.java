/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MAX_FILE_AGE_FOR_READ_MILLIS;
import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.getConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.LogRecordDataSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoContentAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.files.TemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadableFileTest {

  @TempDir File dir;
  private File source;
  private File temporaryFile;
  private ReadableFile readableFile;
  private TimeProvider timeProvider;
  private TemporaryFileProvider temporaryFileProvider;
  private static final long CREATED_TIME_MILLIS = 1000L;
  private static final LogRecordDataSerializer SERIALIZER = SignalSerializer.ofLogs();
  private static final LogRecordData FIRST_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("First log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  private static final LogRecordData SECOND_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Second log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  private static final LogRecordData THIRD_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Third log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  @BeforeEach
  public void setUp() throws IOException {
    source = new File(dir, "sourceFile");
    temporaryFile = new File(dir, "temporaryFile");
    addFileContents(source);
    temporaryFileProvider = mock();
    doReturn(temporaryFile).when(temporaryFileProvider).createTemporaryFile(anyString());
    timeProvider = mock();
    readableFile =
        new ReadableFile(
            source, CREATED_TIME_MILLIS, timeProvider, getConfiguration(temporaryFileProvider));
  }

  private static void addFileContents(File source) throws IOException {
    List<byte[]> lines = new ArrayList<>();
    lines.add(SERIALIZER.serialize(Collections.singleton(FIRST_LOG_RECORD)));
    lines.add(SERIALIZER.serialize(Collections.singleton(SECOND_LOG_RECORD)));
    lines.add(SERIALIZER.serialize(Collections.singleton(THIRD_LOG_RECORD)));

    try (FileOutputStream out = new FileOutputStream(source)) {
      for (byte[] line : lines) {
        out.write(line);
        out.write(Constants.NEW_LINE_BYTES);
      }
    }
  }

  @Test
  public void readSingleLineAndRemoveIt() throws IOException {
    readableFile.readLine(
        bytes -> {
          assertEquals(FIRST_LOG_RECORD, deserialize(bytes));
          return true;
        });

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertEquals(2, logs.size());
    assertEquals(SECOND_LOG_RECORD, logs.get(0));
    assertEquals(THIRD_LOG_RECORD, logs.get(1));
  }

  @Test
  public void deleteTemporaryFileWhenClosing() throws IOException {
    readableFile.readLine(bytes -> true);
    readableFile.close();

    assertFalse(temporaryFile.exists());
  }

  @Test
  public void readMultipleLinesAndRemoveThem() throws IOException {
    readableFile.readLine(bytes -> true);
    readableFile.readLine(bytes -> true);

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertEquals(1, logs.size());
    assertEquals(THIRD_LOG_RECORD, logs.get(0));
  }

  @Test
  public void whenConsumerReturnsFalse_doNotRemoveLineFromSource() throws IOException {
    readableFile.readLine(bytes -> false);

    List<LogRecordData> logs = getRemainingDataAndClose(readableFile);

    assertEquals(3, logs.size());
  }

  @Test
  public void whenReadingLastLine_deleteOriginalFile_and_close() throws IOException {
    getRemainingDataAndClose(readableFile);

    assertFalse(source.exists());
    assertTrue(readableFile.isClosed());
  }

  @Test
  public void whenNoMoreLinesAvailableToRead_deleteOriginalFile_close_and_throwException()
      throws IOException {
    File emptyFile = new File(dir, "emptyFile");
    if (!emptyFile.createNewFile()) {
      fail("Could not create file for tests");
    }

    ReadableFile emptyReadableFile =
        new ReadableFile(
            emptyFile, CREATED_TIME_MILLIS, timeProvider, getConfiguration(temporaryFileProvider));
    try {
      emptyReadableFile.readLine(bytes -> true);
      fail();
    } catch (NoContentAvailableException ignored) {
      assertTrue(emptyReadableFile.isClosed());
      assertFalse(emptyFile.exists());
    }
  }

  @Test
  public void
      whenReadingAfterTheConfiguredReadingTimeExpired_deleteOriginalFile_close_and_throwException()
          throws IOException {
    readableFile.readLine(bytes -> true);
    doReturn(CREATED_TIME_MILLIS + MAX_FILE_AGE_FOR_READ_MILLIS)
        .when(timeProvider)
        .getSystemCurrentTimeMillis();

    try {
      readableFile.readLine(bytes -> true);
      fail();
    } catch (ReadingTimeoutException ignored) {
      assertTrue(readableFile.isClosed());
    }
  }

  @Test
  public void whenReadingAfterClosed_throwException() throws IOException {
    readableFile.readLine(bytes -> true);
    readableFile.close();

    try {
      readableFile.readLine(bytes -> true);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  private static List<LogRecordData> getRemainingDataAndClose(ReadableFile readableFile)
      throws IOException {
    List<LogRecordData> result = new ArrayList<>();
    while (true) {
      try {
        readableFile.readLine(
            bytes -> {
              result.add(deserialize(bytes));
              return true;
            });
      } catch (IOException ignored) {
        break;
      }
    }

    readableFile.close();

    return result;
  }

  private static LogRecordData deserialize(byte[] data) {
    return SERIALIZER.deserialize(data).get(0);
  }
}
