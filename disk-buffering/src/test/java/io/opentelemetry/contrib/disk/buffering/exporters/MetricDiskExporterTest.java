/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetricDiskExporterTest {

  private MetricExporter wrapped;
  private MetricDiskExporter exporter;
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() {
    wrapped = mock();
    exporter = new MetricDiskExporter(wrapped, rootDir, TestData.getDefaultConfiguration());
  }

  @Test
  public void verifyStorageFolderName() {
    assertEquals("metrics", exporter.getStorageFolderName());
  }

  @Test
  public void callWrappedWhenDoingExport() {
    List<MetricData> data = Collections.emptyList();
    CompletableResultCode result = CompletableResultCode.ofSuccess();
    doReturn(result).when(wrapped).export(data);

    assertEquals(result, exporter.doExport(data));

    verify(wrapped).export(data);
  }

  @Test
  public void verifySerializer() {
    assertEquals(SignalSerializer.ofMetrics(), exporter.getSerializer());
  }

  @Test
  public void onFlush_flushWrappedExporter() {
    exporter.flush();

    verify(wrapped).flush();
  }

  @Test
  public void provideWrappedAggregationTemporality() {
    InstrumentType instrumentType = mock();
    AggregationTemporality aggregationTemporality = AggregationTemporality.DELTA;
    doReturn(aggregationTemporality).when(wrapped).getAggregationTemporality(instrumentType);

    assertEquals(aggregationTemporality, exporter.getAggregationTemporality(instrumentType));

    verify(wrapped).getAggregationTemporality(instrumentType);
  }
}
