package io.opentelemetry.contrib.disk.buffering.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpanDiskExporterTest {
  private SpanExporter wrapped;
  private SpanDiskExporter exporter;
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() {
    wrapped = mock();
    exporter = new SpanDiskExporter(wrapped, rootDir, TestData.CONFIGURATION);
  }

  @Test
  public void verifyStorageFolderName() {
    assertEquals("spans", exporter.getStorageFolderName());
  }

  @Test
  public void callWrappedWhenDoingExport() {
    List<SpanData> data = Collections.emptyList();
    CompletableResultCode result = CompletableResultCode.ofSuccess();
    doReturn(result).when(wrapped).export(data);

    assertEquals(result, exporter.doExport(data));

    verify(wrapped).export(data);
  }

  @Test
  public void verifySerializer() {
    assertEquals(SignalSerializer.ofSpans(), exporter.getSerializer());
  }

  @Test
  public void onFlush_flushWrappedExporter() {
    exporter.flush();

    verify(wrapped).flush();
  }
}
