/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.disk.buffering.exporters.LogRecordToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.exporters.MetricToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.exporters.SpanToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileLogRecordStorage;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileMetricStorage;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileSpanStorage;
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileStorageConfiguration;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {
  private Tracer tracer;
  private SdkMeterProvider meterProvider;
  private Meter meter;
  private Logger logger;
  private SignalStorage.Span spanStorage;
  private SignalStorage.LogRecord logStorage;
  private SignalStorage.Metric metricStorage;
  private SpanToDiskExporter spanToDiskExporter;
  private MetricToDiskExporter metricToDiskExporter;
  private LogRecordToDiskExporter logToDiskExporter;
  @Mock private ExporterCallback<SpanData> spanCallback;
  @Mock private ExporterCallback<LogRecordData> logCallback;
  @Mock private ExporterCallback<MetricData> metricCallback;
  @TempDir private File rootDir;
  private static final long DELAY_BEFORE_READING_MILLIS = 500;

  @BeforeEach
  void setUp() {
    FileStorageConfiguration storageConfig =
        FileStorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(DELAY_BEFORE_READING_MILLIS - 1)
            .setMinFileAgeForReadMillis(DELAY_BEFORE_READING_MILLIS)
            .build();

    // Setting up spans
    spanStorage = FileSpanStorage.create(new File(rootDir, "spans"), storageConfig);
    spanToDiskExporter =
        SpanToDiskExporter.builder(spanStorage).setExporterCallback(spanCallback).build();
    tracer = createTracerProvider(spanToDiskExporter).get("SpanInstrumentationScope");

    // Setting up metrics
    metricStorage = FileMetricStorage.create(new File(rootDir, "metrics"), storageConfig);
    metricToDiskExporter =
        MetricToDiskExporter.builder(metricStorage).setExporterCallback(metricCallback).build();
    meterProvider = createMeterProvider(metricToDiskExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    // Setting up logs
    logStorage = FileLogRecordStorage.create(new File(rootDir, "logs"), storageConfig);
    logToDiskExporter =
        LogRecordToDiskExporter.builder(logStorage).setExporterCallback(logCallback).build();
    logger = createLoggerProvider(logToDiskExporter).get("LogInstrumentationScope");
  }

  @AfterEach
  void tearDown() throws IOException {
    // Closing span exporter
    spanToDiskExporter.shutdown();
    verify(spanCallback).onShutdown();
    verifyNoMoreInteractions(spanCallback);

    // Closing log exporter
    logToDiskExporter.shutdown();
    verify(logCallback).onShutdown();
    verifyNoMoreInteractions(spanCallback);

    // Closing metric exporter
    metricToDiskExporter.shutdown();
    verify(metricCallback).onShutdown();
    verifyNoMoreInteractions(spanCallback);

    // Closing storages
    spanStorage.close();
    logStorage.close();
    metricStorage.close();
  }

  @Test
  void verifyIntegration() throws InterruptedException {
    // Creating span
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();
    verify(spanCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(spanCallback);

    // Creating log
    logger.logRecordBuilder().setBody("Log body").emit();
    verify(logCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(spanCallback);

    // Creating metric
    meter.counterBuilder("counter").build().add(1);
    meterProvider.forceFlush();
    verify(metricCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(spanCallback);

    // Waiting for read time
    sleep(DELAY_BEFORE_READING_MILLIS);

    // Read
    List<SpanData> storedSpans = new ArrayList<>();
    List<LogRecordData> storedLogs = new ArrayList<>();
    List<MetricData> storedMetrics = new ArrayList<>();
    spanStorage.forEach(storedSpans::addAll);
    logStorage.forEach(storedLogs::addAll);
    metricStorage.forEach(storedMetrics::addAll);

    assertEquals(1, storedSpans.size());
    assertEquals(1, storedLogs.size());
    assertEquals(1, storedMetrics.size());
  }

  private static SdkTracerProvider createTracerProvider(SpanExporter exporter) {
    return SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build();
  }

  private static SdkMeterProvider createMeterProvider(MetricExporter exporter) {
    return SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.create(exporter))
        .build();
  }

  private static SdkLoggerProvider createLoggerProvider(LogRecordExporter exporter) {
    return SdkLoggerProvider.builder()
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
        .build();
  }
}
