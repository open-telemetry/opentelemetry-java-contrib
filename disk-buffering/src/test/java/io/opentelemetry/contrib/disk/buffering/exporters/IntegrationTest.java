package io.opentelemetry.contrib.disk.buffering.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
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
  private TimeProvider timeMachine;
  @TempDir File rootDir;
  private static final long INITIAL_TIME_IN_MILLIS = 1000;
  private static final StorageConfiguration STORAGE_CONFIGURATION =
      StorageConfiguration.getDefault();

  @BeforeEach
  public void setUp() throws IOException {
    timeMachine = mock();
    doReturn(INITIAL_TIME_IN_MILLIS).when(timeMachine).getSystemCurrentTimeMillis();

    // Setting up spans
    memorySpanExporter = InMemorySpanExporter.create();
    diskSpanExporter =
        new SpanDiskExporter(memorySpanExporter, rootDir, STORAGE_CONFIGURATION, timeMachine);
    tracer = createTracerProvider(diskSpanExporter).get("SpanInstrumentationScope");

    // Setting up metrics
    memoryMetricExporter = InMemoryMetricExporter.create();
    diskMetricExporter =
        new MetricDiskExporter(memoryMetricExporter, rootDir, STORAGE_CONFIGURATION, timeMachine);
    meterProvider = createMeterProvider(diskMetricExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    // Setting up logs
    memoryLogRecordExporter = InMemoryLogRecordExporter.create();
    diskLogRecordExporter =
        new LogRecordDiskExporter(
            memoryLogRecordExporter, rootDir, STORAGE_CONFIGURATION, timeMachine);
    logger = createLoggerProvider(diskLogRecordExporter).get("LogInstrumentationScope");
  }

  @Test
  public void verifySpansIntegration() throws IOException {
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();

    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, memorySpanExporter.getFinishedSpanItems().size());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(STORAGE_CONFIGURATION.getMinFileAgeForReadMillis());

    // Read and send stored data.
    assertTrue(diskSpanExporter.exportStoredBatch(1, TimeUnit.SECONDS));

    // Now the data must have been delegated to the original exporter.
    assertEquals(1, memorySpanExporter.getFinishedSpanItems().size());

    // Bonus: Try to read again, no more data should be available.
    assertFalse(diskSpanExporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertEquals(1, memorySpanExporter.getFinishedSpanItems().size());
  }

  @Test
  public void verifyMetricsIntegration() throws IOException {
    meter.counterBuilder("Counter").build().add(2);
    meterProvider.forceFlush();

    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, memoryMetricExporter.getFinishedMetricItems().size());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(STORAGE_CONFIGURATION.getMinFileAgeForReadMillis());

    // Read and send stored data.
    assertTrue(diskMetricExporter.exportStoredBatch(1, TimeUnit.SECONDS));

    // Now the data must have been delegated to the original exporter.
    assertEquals(1, memoryMetricExporter.getFinishedMetricItems().size());

    // Bonus: Try to read again, no more data should be available.
    assertFalse(diskMetricExporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertEquals(1, memoryMetricExporter.getFinishedMetricItems().size());
  }

  @Test
  public void verifyLogRecordsIntegration() throws IOException {
    logger.logRecordBuilder().setBody("I'm a log!").emit();

    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, memoryLogRecordExporter.getFinishedLogRecordItems().size());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(STORAGE_CONFIGURATION.getMinFileAgeForReadMillis());

    // Read and send stored data.
    assertTrue(diskLogRecordExporter.exportStoredBatch(1, TimeUnit.SECONDS));

    // Now the data must have been delegated to the original exporter.
    assertEquals(1, memoryLogRecordExporter.getFinishedLogRecordItems().size());

    // Bonus: Try to read again, no more data should be available.
    assertFalse(diskLogRecordExporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertEquals(1, memoryLogRecordExporter.getFinishedLogRecordItems().size());
  }

  @SuppressWarnings("DirectInvocationOnMock")
  private void fastForwardTimeByMillis(long milliseconds) {
    doReturn(timeMachine.getSystemCurrentTimeMillis() + milliseconds)
        .when(timeMachine)
        .getSystemCurrentTimeMillis();
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
