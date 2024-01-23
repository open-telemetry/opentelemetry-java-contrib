/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogRecordToDiskExporterTest {

  @Mock private ToDiskExporter<LogRecordData> delegate;

  @Test
  void delegateShutdown_success() throws IOException {
    LogRecordToDiskExporter testClass = new LogRecordToDiskExporter(delegate);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isTrue();
    verify(delegate).shutdown();
  }

  @Test
  void delegateShutdown_fail() throws IOException {
    doThrow(new IOException("boom")).when(delegate).shutdown();
    LogRecordToDiskExporter testClass = new LogRecordToDiskExporter(delegate);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isFalse();
    verify(delegate).shutdown();
  }

  @Test
  void delegateExport() {
    LogRecordData log1 = mock();
    LogRecordData log2 = mock();
    List<LogRecordData> logRecords = Arrays.asList(log1, log2);

    LogRecordToDiskExporter testClass = new LogRecordToDiskExporter(delegate);
    testClass.export(logRecords);

    verify(delegate).export(logRecords);
  }

  @Test
  void flushReturnsSuccess() {
    LogRecordToDiskExporter testClass = new LogRecordToDiskExporter(delegate);
    CompletableResultCode result = testClass.flush();
    assertThat(result.isSuccess()).isTrue();
  }
}
