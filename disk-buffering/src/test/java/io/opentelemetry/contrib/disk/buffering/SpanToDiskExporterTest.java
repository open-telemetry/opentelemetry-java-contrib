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
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanToDiskExporterTest {
  @Mock private ToDiskExporter<SpanData> delegate;

  @Test
  void delegateShutdown_success() throws IOException {
    SpanToDiskExporter testClass = new SpanToDiskExporter(delegate);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isTrue();
    verify(delegate).shutdown();
  }

  @Test
  void delegateShutdown_fail() throws IOException {
    doThrow(new IOException("boom")).when(delegate).shutdown();
    SpanToDiskExporter testClass = new SpanToDiskExporter(delegate);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isFalse();
    verify(delegate).shutdown();
  }

  @Test
  void delegateExport() {
    SpanData span1 = mock();
    SpanData span2 = mock();
    List<SpanData> spans = Arrays.asList(span1, span2);

    SpanToDiskExporter testClass = new SpanToDiskExporter(delegate);
    testClass.export(spans);

    verify(delegate).export(spans);
  }

  @Test
  void flushReturnsSuccess() {
    SpanToDiskExporter testClass = new SpanToDiskExporter(delegate);
    CompletableResultCode result = testClass.flush();
    assertThat(result.isSuccess()).isTrue();
  }
}
