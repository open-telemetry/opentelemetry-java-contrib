/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetricDiskExporterTest {

  private MetricExporter wrapped;
  private MetricDiskExporter exporter;
  private static final StorageConfiguration STORAGE_CONFIGURATION =
      TestData.getDefaultConfiguration();
  private static final String STORAGE_FOLDER_NAME = "metrics";
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() throws IOException {
    wrapped = mock();
    exporter = new MetricDiskExporter(wrapped, rootDir, STORAGE_CONFIGURATION);
  }

  @Test
  public void verifyCacheFolderName() {
    File[] files = rootDir.listFiles();
    assertEquals(1, files.length);
    assertEquals(STORAGE_FOLDER_NAME, files[0].getName());
  }

  @Test
  public void onFlush_returnSuccess() {
    assertEquals(CompletableResultCode.ofSuccess(), exporter.flush());
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
