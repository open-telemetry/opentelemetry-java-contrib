/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricToDiskExporterTest {

  @Mock private ToDiskExporter<MetricData> delegate;

  @Test
  void delegateShutdown_success() throws IOException {
    MetricToDiskExporter testClass =
        new MetricToDiskExporter(delegate, MetricToDiskExporterTest::temporalityFn);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isTrue();
    verify(delegate).shutdown();
  }

  private static AggregationTemporality temporalityFn(InstrumentType instrumentType) {
    return CUMULATIVE;
  }

  @Test
  void delegateShutdown_fail() throws IOException {
    doThrow(new IOException("boom")).when(delegate).shutdown();
    MetricToDiskExporter testClass =
        new MetricToDiskExporter(delegate, MetricToDiskExporterTest::temporalityFn);
    CompletableResultCode result = testClass.shutdown();
    assertThat(result.isSuccess()).isFalse();
    verify(delegate).shutdown();
  }

  @Test
  void delegateExport() {
    MetricData metric1 = mock();
    MetricData metric2 = mock();
    List<MetricData> metrics = Arrays.asList(metric1, metric2);

    MetricToDiskExporter testClass =
        new MetricToDiskExporter(delegate, MetricToDiskExporterTest::temporalityFn);

    testClass.export(metrics);

    verify(delegate).export(metrics);
  }

  @Test
  void flushReturnsSuccess() {
    MetricToDiskExporter testClass =
        new MetricToDiskExporter(delegate, MetricToDiskExporterTest::temporalityFn);

    CompletableResultCode result = testClass.flush();
    assertThat(result.isSuccess()).isTrue();
  }
}
