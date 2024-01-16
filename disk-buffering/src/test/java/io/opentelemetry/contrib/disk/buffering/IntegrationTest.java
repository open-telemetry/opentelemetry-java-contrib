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
import io.opentelemetry.contrib.disk.buffering.internal.exporters.LogRecordDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.MetricDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.SpanDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.FromDiskExporter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IntegrationTest {
  private InMemorySpanExporter memorySpanExporter;
  private SpanDiskExporter diskSpanExporter;
  private Tracer tracer;
  private InMemoryMetricExporter memoryMetricExporter;
  private MetricDiskExporter diskMetricExporter;
  private SdkMeterProvider meterProvider;
  private Meter meter;
  private InMemoryLogRecordExporter memoryLogRecordExporter;
  private LogRecordDiskExporter diskLogRecordExporter;
  private Logger logger;
  private Clock clock;
  @TempDir File rootDir;
  private static final long INITIAL_TIME_IN_MILLIS = 1000;
  private static final StorageConfiguration STORAGE_CONFIGURATION =
      StorageConfiguration.getDefault();

  @BeforeEach
  void setUp() throws IOException {
    clock = mock();
    doReturn(MILLISECONDS.toNanos(INITIAL_TIME_IN_MILLIS)).when(clock).now();

    // Setting up spans
    memorySpanExporter = InMemorySpanExporter.create();
    diskSpanExporter =
        SpanDiskExporter.create(memorySpanExporter, rootDir, STORAGE_CONFIGURATION, clock);
    tracer = createTracerProvider(diskSpanExporter).get("SpanInstrumentationScope");

    // Setting up metrics
    memoryMetricExporter = InMemoryMetricExporter.create();
    diskMetricExporter =
        MetricDiskExporter.create(memoryMetricExporter, rootDir, STORAGE_CONFIGURATION, clock);
    meterProvider = createMeterProvider(diskMetricExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    // Setting up logs
    memoryLogRecordExporter = InMemoryLogRecordExporter.create();
    diskLogRecordExporter =
        LogRecordDiskExporter.create(
            memoryLogRecordExporter, rootDir, STORAGE_CONFIGURATION, clock);
    logger = createLoggerProvider(diskLogRecordExporter).get("LogInstrumentationScope");
  }

  @Test
  void verifySpansIntegration() throws IOException {
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();

    assertExporter(diskSpanExporter, () -> memorySpanExporter.getFinishedSpanItems().size());
  }

  @Test
  void verifyMetricsIntegration() throws IOException {
    meter.counterBuilder("Counter").build().add(2);
    meterProvider.forceFlush();

    assertExporter(diskMetricExporter, () -> memoryMetricExporter.getFinishedMetricItems().size());
  }

  @Test
  void verifyLogRecordsIntegration() throws IOException {
    logger.logRecordBuilder().setBody("I'm a log!").emit();

    assertExporter(
        diskLogRecordExporter, () -> memoryLogRecordExporter.getFinishedLogRecordItems().size());
  }

  private void assertExporter(FromDiskExporter exporter, Supplier<Integer> finishedItems)
      throws IOException {
    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, finishedItems.get());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(STORAGE_CONFIGURATION.getMinFileAgeForReadMillis());

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
