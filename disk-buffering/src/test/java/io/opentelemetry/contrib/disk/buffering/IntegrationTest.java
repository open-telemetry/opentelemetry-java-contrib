/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IntegrationTest {
  private InMemorySpanExporter memorySpanExporter;
  private SpanToDiskExporter spanToDiskExporter;
  private Tracer tracer;
  private InMemoryMetricExporter memoryMetricExporter;
  private MetricToDiskExporter metricToDiskExporter;
  private SdkMeterProvider meterProvider;
  private Meter meter;
  private InMemoryLogRecordExporter memoryLogRecordExporter;
  private LogRecordToDiskExporter logToDiskExporter;
  private Logger logger;
  private Clock clock;
  @TempDir File rootDir;
  private static final long INITIAL_TIME_IN_MILLIS = 1000;
  private StorageConfiguration storageConfig;

  @BeforeEach
  void setUp() throws IOException {
    storageConfig = StorageConfiguration.getDefault(rootDir);
    clock = mock();

    doReturn(MILLISECONDS.toNanos(INITIAL_TIME_IN_MILLIS)).when(clock).now();

    // Setting up spans
    memorySpanExporter = InMemorySpanExporter.create();
    ToDiskExporter<SpanData> toDiskSpanExporter =
        buildToDiskExporter(SignalSerializer.ofSpans(), memorySpanExporter::export);
    spanToDiskExporter = new SpanToDiskExporter(toDiskSpanExporter);
    tracer = createTracerProvider(spanToDiskExporter).get("SpanInstrumentationScope");

    // Setting up metrics
    memoryMetricExporter = InMemoryMetricExporter.create();
    ToDiskExporter<MetricData> toDiskMetricExporter =
        buildToDiskExporter(SignalSerializer.ofMetrics(), memoryMetricExporter::export);
    metricToDiskExporter =
        new MetricToDiskExporter(
            toDiskMetricExporter, memoryMetricExporter::getAggregationTemporality);
    meterProvider = createMeterProvider(metricToDiskExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    // Setting up logs
    memoryLogRecordExporter = InMemoryLogRecordExporter.create();
    ToDiskExporter<LogRecordData> toDiskLogExporter =
        buildToDiskExporter(SignalSerializer.ofLogs(), memoryLogRecordExporter::export);
    logToDiskExporter = new LogRecordToDiskExporter(toDiskLogExporter);
    logger = createLoggerProvider(logToDiskExporter).get("LogInstrumentationScope");
  }

  @NotNull
  private <T> ToDiskExporter<T> buildToDiskExporter(
      SignalSerializer<T> serializer, Function<Collection<T>, CompletableResultCode> exporter)
      throws IOException {
    return ToDiskExporter.<T>builder()
        .setFolderName("spans")
        .setStorageConfiguration(storageConfig)
        .setSerializer(serializer)
        .setExportFunction(exporter)
        .setStorageClock(clock)
        .build();
  }

  @NotNull
  private <T> FromDiskExporter<T> buildFromDiskExporter(
      FromDiskExporterBuilder<T> builder,
      Function<Collection<T>, CompletableResultCode> exportFunction,
      SignalSerializer<T> serializer)
      throws IOException {
    return builder
        .setExportFunction(exportFunction)
        .setFolderName("spans")
        .setStorageConfiguration(storageConfig)
        .setDeserializer(serializer)
        .setStorageClock(clock)
        .build();
  }

  @Test
  void verifySpansIntegration() throws IOException {
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();
    FromDiskExporter<SpanData> fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporter.builder(), memorySpanExporter::export, SignalSerializer.ofSpans());
    assertExporter(fromDiskExporter, () -> memorySpanExporter.getFinishedSpanItems().size());
  }

  @Test
  void verifyMetricsIntegration() throws IOException {
    meter.counterBuilder("Counter").build().add(2);
    meterProvider.forceFlush();

    FromDiskExporter<MetricData> fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporter.builder(), memoryMetricExporter::export, SignalSerializer.ofMetrics());
    assertExporter(fromDiskExporter, () -> memoryMetricExporter.getFinishedMetricItems().size());
  }

  @Test
  void verifyLogRecordsIntegration() throws IOException {
    logger.logRecordBuilder().setBody("I'm a log!").emit();

    FromDiskExporter<LogRecordData> fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporter.builder(), memoryLogRecordExporter::export, SignalSerializer.ofLogs());
    assertExporter(
        fromDiskExporter, () -> memoryLogRecordExporter.getFinishedLogRecordItems().size());
  }

  private <T> void assertExporter(FromDiskExporter<T> exporter, Supplier<Integer> finishedItems)
      throws IOException {
    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, finishedItems.get());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(storageConfig.getMinFileAgeForReadMillis());

    // Read and send stored data.
    assertTrue(exporter.exportStoredBatch(1, TimeUnit.SECONDS));

    // Now the data must have been delegated to the original exporter.
    assertEquals(1, finishedItems.get());

    // Bonus: Try to read again, no more data should be available.
    assertFalse(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertEquals(1, finishedItems.get());
  }

  @SuppressWarnings("DirectInvocationOnMock")
  private void fastForwardTimeByMillis(long milliseconds) {
    doReturn(clock.now() + MILLISECONDS.toNanos(milliseconds)).when(clock).now();
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
