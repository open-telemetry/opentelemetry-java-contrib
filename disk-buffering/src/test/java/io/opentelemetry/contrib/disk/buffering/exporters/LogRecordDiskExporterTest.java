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
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogRecordDiskExporterTest {
  private LogRecordExporter wrapped;
  private LogRecordDiskExporter exporter;
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() {
    wrapped = mock();
    exporter = new LogRecordDiskExporter(wrapped, rootDir, TestData.getDefaultConfiguration());
  }

  @Test
  public void verifyStorageFolderName() {
    assertEquals("logs", exporter.getStorageFolderName());
  }

  @Test
  public void callWrappedWhenDoingExport() {
    List<LogRecordData> data = Collections.emptyList();
    CompletableResultCode result = CompletableResultCode.ofSuccess();
    doReturn(result).when(wrapped).export(data);

    assertEquals(result, exporter.doExport(data));

    verify(wrapped).export(data);
  }

  @Test
  public void verifySerializer() {
    assertEquals(SignalSerializer.ofLogs(), exporter.getSerializer());
  }

  @Test
  public void onFlush_flushWrappedExporter() {
    exporter.flush();

    verify(wrapped).flush();
  }
}
