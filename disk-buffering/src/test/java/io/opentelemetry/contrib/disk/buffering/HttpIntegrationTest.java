/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.exporter.internal.http.HttpExporterBuilder;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.StandardComponentId;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HttpIntegrationTest {
  private Tracer tracer;
  private SdkMeterProvider meterProvider;
  private Meter meter;
  private Logger logger;
  private Clock clock;
  @TempDir File rootDir;
  private static final long INITIAL_TIME_IN_MILLIS = 1000;
  private static final long NOW_NANOS = MILLISECONDS.toNanos(INITIAL_TIME_IN_MILLIS);
  private static final String TRACES_PATH = "/v1/traces";
  private static final String METRICS_PATH = "/v1/metrics";
  private static final String LOGS_PATH = "/v1/logs";
  private StorageConfiguration storageConfig;
  private Storage spanStorage;
  private final List<Object> exportedData = new CopyOnWriteArrayList<>();
  private final CountDownLatch latch = new CountDownLatch(1);

  final HttpHandler tracesHandler =
      exchange -> {
        InputStream requestBody = exchange.getRequestBody();
        ExportTraceServiceRequest otlpData = ExportTraceServiceRequest.ADAPTER.decode(requestBody);
        exportedData.addAll(otlpData.resource_spans);
        latch.countDown();
        String response = "OK";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response.getBytes(UTF_8));
        responseBody.close();
      };

  final HttpHandler logsHandler =
      exchange -> {
        InputStream requestBody = exchange.getRequestBody();
        ExportLogsServiceRequest otlpData = ExportLogsServiceRequest.ADAPTER.decode(requestBody);
        exportedData.addAll(otlpData.resource_logs);
        latch.countDown();
        String response = "OK";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response.getBytes(UTF_8));
        responseBody.close();
      };

  final HttpHandler metricsHandler =
      exchange -> {
        InputStream requestBody = exchange.getRequestBody();
        ExportMetricsServiceRequest otlpData =
            ExportMetricsServiceRequest.ADAPTER.decode(requestBody);
        exportedData.addAll(otlpData.resource_metrics);
        latch.countDown();
        String response = "OK";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response.getBytes(UTF_8));
        responseBody.close();
      };
  private HttpServer mockServer;
  private int port;

  @BeforeEach
  void setUp() throws IOException {
    clock = mock();
    storageConfig =
        StorageConfiguration.builder().setRootDir(rootDir).setDebugEnabled(true).build();
    spanStorage =
        Storage.builder(SignalTypes.spans)
            .setStorageConfiguration(storageConfig)
            .setStorageClock(clock)
            .build();

    when(clock.now()).thenReturn(NOW_NANOS);

    mockServer = HttpServer.create(new InetSocketAddress(0), 0);
    port = mockServer.getAddress().getPort();
    mockServer.createContext(TRACES_PATH, tracesHandler);
    mockServer.createContext(LOGS_PATH, logsHandler);
    mockServer.createContext(METRICS_PATH, metricsHandler);
    mockServer.start();

    // Setting up spans
    OtlpHttpSpanExporter spanExporter =
        OtlpHttpSpanExporter.builder()
            .setEndpoint("http://localhost:" + port + TRACES_PATH)
            .build();
    ToDiskExporter<SpanData> toDiskSpanExporter =
        buildToDiskExporter(SignalSerializer.ofSpans(), spanExporter::export);
    SpanToDiskExporter spanToDiskExporter = new SpanToDiskExporter(toDiskSpanExporter);
    tracer = createTracerProvider(spanToDiskExporter).get("SpanInstrumentationScope");

    // Setting up metrics
    OtlpHttpMetricExporter metricExporter =
        OtlpHttpMetricExporter.builder()
            .setEndpoint("http://localhost:" + port + METRICS_PATH)
            .build();
    ToDiskExporter<MetricData> toDiskMetricExporter =
        buildToDiskExporter(SignalSerializer.ofMetrics(), metricExporter::export);
    MetricToDiskExporter metricToDiskExporter =
        new MetricToDiskExporter(toDiskMetricExporter, metricExporter);
    meterProvider = createMeterProvider(metricToDiskExporter);
    meter = meterProvider.get("MetricInstrumentationScope");

    // Setting up logs
    OtlpHttpLogRecordExporter logRecordExporter =
        OtlpHttpLogRecordExporter.builder()
            .setEndpoint("http://localhost:" + port + LOGS_PATH)
            .build();
    ToDiskExporter<LogRecordData> toDiskLogExporter =
        buildToDiskExporter(SignalSerializer.ofLogs(), logRecordExporter::export);
    LogRecordToDiskExporter logToDiskExporter = new LogRecordToDiskExporter(toDiskLogExporter);
    logger = createLoggerProvider(logToDiskExporter).get("LogInstrumentationScope");
  }

  @AfterEach
  void tearDown() throws IOException {
    spanStorage.close();
    mockServer.stop(0);
  }

  @NotNull
  private <T> ToDiskExporter<T> buildToDiskExporter(
      SignalSerializer<T> serializer, Function<Collection<T>, CompletableResultCode> exporter) {
    return ToDiskExporter.<T>builder(spanStorage)
        .setSerializer(serializer)
        .setExportFunction(exporter)
        .build();
  }

  @NotNull
  private static <T> FromDiskExporterImpl buildFromDiskExporter(
      FromDiskExporterBuilder<T> builder,
      StandardComponentId.ExporterType exporterType,
      String endpoint)
      throws IOException {
    return builder.setExporter(new HttpExporterBuilder<>(exporterType, endpoint).build()).build();
  }

  @Test
  void verifySpansIntegration() throws IOException, InterruptedException {
    Span span = tracer.spanBuilder("Span name").startSpan();
    span.end();
    FromDiskExporterImpl fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporterImpl.builder(spanStorage, SignalTypes.spans),
            StandardComponentId.ExporterType.OTLP_HTTP_SPAN_EXPORTER,
            "http://localhost:" + port + TRACES_PATH);
    assertExporter(fromDiskExporter);
  }

  @Test
  void verifyMetricsIntegration() throws IOException, InterruptedException {
    meter.counterBuilder("Counter").build().add(2);
    meterProvider.forceFlush();

    FromDiskExporterImpl fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporterImpl.builder(spanStorage, SignalTypes.metrics),
            StandardComponentId.ExporterType.OTLP_HTTP_METRIC_EXPORTER,
            "http://localhost:" + port + METRICS_PATH);
    assertExporter(fromDiskExporter);
  }

  @Test
  void verifyLogRecordsIntegration() throws IOException, InterruptedException {
    logger.logRecordBuilder().setBody("I'm a log!").emit();

    FromDiskExporterImpl fromDiskExporter =
        buildFromDiskExporter(
            FromDiskExporterImpl.builder(spanStorage, SignalTypes.logs),
            StandardComponentId.ExporterType.OTLP_HTTP_LOG_EXPORTER,
            "http://localhost:" + port + LOGS_PATH);
    assertExporter(fromDiskExporter);
  }

  private void assertExporter(FromDiskExporterImpl exporter)
      throws IOException, InterruptedException {
    // Verify no data has been received in the original exporter until this point.
    assertEquals(0, exportedData.size());

    // Go to the future when we can read the stored items.
    fastForwardTimeByMillis(storageConfig.getMinFileAgeForReadMillis());

    // Read and send stored data.
    assertTrue(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertTrue(latch.await(10, SECONDS));

    // Now the data must have been delegated to the original exporter.
    assertEquals(1, exportedData.size());

    // Bonus: Try to read again, no more data should be available.
    assertFalse(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
    assertEquals(1, exportedData.size());
  }

  @SuppressWarnings("DirectInvocationOnMock")
  private void fastForwardTimeByMillis(long milliseconds) {
    when(clock.now()).thenReturn(NOW_NANOS + MILLISECONDS.toNanos(milliseconds));
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
