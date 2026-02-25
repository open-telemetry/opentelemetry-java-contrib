/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.clearInvocations;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
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
  private File spansDir;
  private File logsDir;
  private File metricsDir;
  private static final long DELAY_BEFORE_READING_MILLIS = 500;
  private static final long MAX_WRITING_TIME_MILLIS = 200;

  @BeforeEach
  void setUp() {
    initStorage(
        FileStorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(MAX_WRITING_TIME_MILLIS)
            .setMinFileAgeForReadMillis(DELAY_BEFORE_READING_MILLIS)
            .build());
  }

  private void initStorage(FileStorageConfiguration storageConfig) {
    spansDir = new File(rootDir, "spans");
    spanStorage = FileSpanStorage.create(spansDir, storageConfig);
    spanToDiskExporter =
        SpanToDiskExporter.builder(spanStorage).setExporterCallback(spanCallback).build();
    tracer = createTracerProvider(spanToDiskExporter).get("SpanInstrumentationScope");

    metricsDir = new File(rootDir, "metrics");
    metricStorage = FileMetricStorage.create(metricsDir, storageConfig);
    metricToDiskExporter =
        MetricToDiskExporter.builder(metricStorage).setExporterCallback(metricCallback).build();
    meterProvider = createMeterProvider(metricToDiskExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    logsDir = new File(rootDir, "logs");
    logStorage = FileLogRecordStorage.create(logsDir, storageConfig);
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
  void verifyIntegration_defaultAutoDelete() throws InterruptedException {
    // Writing to first file
    createSpan();
    createLog();
    createMetric();

    // Waiting to write on second file
    sleep(MAX_WRITING_TIME_MILLIS);

    // Writing to second file
    createSpan();
    createLog();
    createMetric();

    // Waiting for read time
    sleep(DELAY_BEFORE_READING_MILLIS);

    // Read (default: items auto-deleted during iteration)
    List<SpanData> storedSpans = new ArrayList<>();
    List<LogRecordData> storedLogs = new ArrayList<>();
    List<MetricData> storedMetrics = new ArrayList<>();
    spanStorage.forEach(storedSpans::addAll);
    logStorage.forEach(storedLogs::addAll);
    metricStorage.forEach(storedMetrics::addAll);

    assertThat(storedSpans).hasSize(2);
    assertThat(storedLogs).hasSize(2);
    assertThat(storedMetrics).hasSize(2);

    // Data is auto-deleted from disk
    assertDirectoryFileCount(spansDir, 0);
    assertDirectoryFileCount(logsDir, 0);
    assertDirectoryFileCount(metricsDir, 0);
  }

  @Test
  void verifyIntegration_withoutAutoDelete() throws InterruptedException {
    initStorage(
        FileStorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(MAX_WRITING_TIME_MILLIS)
            .setMinFileAgeForReadMillis(DELAY_BEFORE_READING_MILLIS)
            .setDeleteItemsOnIteration(false)
            .build());

    // Writing to first file
    createSpan();
    createLog();
    createMetric();

    // Waiting to write on second file
    sleep(MAX_WRITING_TIME_MILLIS);

    // Writing to second file
    createSpan();
    createLog();
    createMetric();

    // Waiting for read time
    sleep(DELAY_BEFORE_READING_MILLIS);

    // Read (items not auto-deleted)
    List<SpanData> storedSpans = new ArrayList<>();
    List<LogRecordData> storedLogs = new ArrayList<>();
    List<MetricData> storedMetrics = new ArrayList<>();
    spanStorage.forEach(storedSpans::addAll);
    logStorage.forEach(storedLogs::addAll);
    metricStorage.forEach(storedMetrics::addAll);

    assertThat(storedSpans).hasSize(2);
    assertThat(storedLogs).hasSize(2);
    assertThat(storedMetrics).hasSize(2);

    // Data stays on disk
    assertDirectoryFileCount(spansDir, 2);
    assertDirectoryFileCount(logsDir, 2);
    assertDirectoryFileCount(metricsDir, 2);
  }

  @Test
  void verifyIntegration_withoutAutoDelete_explicitRemove() throws InterruptedException {
    initStorage(
        FileStorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(MAX_WRITING_TIME_MILLIS)
            .setMinFileAgeForReadMillis(DELAY_BEFORE_READING_MILLIS)
            .setDeleteItemsOnIteration(false)
            .build());

    // Writing to first file
    createSpan();
    createLog();
    createMetric();

    // Waiting to write on second file
    sleep(MAX_WRITING_TIME_MILLIS);

    // Writing to second file
    createSpan();
    createLog();
    createMetric();

    // Waiting for read time
    sleep(DELAY_BEFORE_READING_MILLIS);

    // Read with explicit removal
    List<SpanData> storedSpans = new ArrayList<>();
    List<LogRecordData> storedLogs = new ArrayList<>();
    List<MetricData> storedMetrics = new ArrayList<>();
    Iterator<Collection<SpanData>> spanIterator = spanStorage.iterator();
    while (spanIterator.hasNext()) {
      storedSpans.addAll(spanIterator.next());
      spanIterator.remove();
    }
    Iterator<Collection<LogRecordData>> logIterator = logStorage.iterator();
    while (logIterator.hasNext()) {
      storedLogs.addAll(logIterator.next());
      logIterator.remove();
    }
    Iterator<Collection<MetricData>> metricIterator = metricStorage.iterator();
    while (metricIterator.hasNext()) {
      storedMetrics.addAll(metricIterator.next());
      metricIterator.remove();
    }

    assertThat(storedSpans).hasSize(2);
    assertThat(storedLogs).hasSize(2);
    assertThat(storedMetrics).hasSize(2);

    // Data explicitly cleared
    assertDirectoryFileCount(spansDir, 0);
    assertDirectoryFileCount(logsDir, 0);
    assertDirectoryFileCount(metricsDir, 0);
  }

  private void createSpan() {
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();
    verify(spanCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(spanCallback);
    clearInvocations(spanCallback);
  }

  private void createLog() {
    logger.logRecordBuilder().setBody("Log body").emit();
    verify(logCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(logCallback);
    clearInvocations(logCallback);
  }

  private void createMetric() {
    meter.counterBuilder("counter").build().add(1);
    meterProvider.forceFlush();
    verify(metricCallback).onExportSuccess(anyCollection());
    verifyNoMoreInteractions(metricCallback);
    clearInvocations(metricCallback);
  }

  private static void assertDirectoryFileCount(File directory, int fileCount) {
    assertThat(directory).isDirectory();
    assertThat(directory.listFiles()).hasSize(fileCount);
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
