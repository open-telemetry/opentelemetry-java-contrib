package io.opentelemetry.contrib.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilteringLogRecordProcessorTest {

  private final InMemoryLogRecordExporter memoryLogRecordExporter = InMemoryLogRecordExporter.create();;
  private final LogRecordProcessor logRecordProcessor =  SimpleLogRecordProcessor.create(memoryLogRecordExporter);;
  private final InMemorySpanExporter spansExporter = InMemorySpanExporter.create();
  private AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder ;
  private Logger logger;


  @BeforeEach
  void setUp() {
    sdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
    sdkBuilder.addPropertiesSupplier(()->{
          Map<String, String> configMap = new HashMap<>();
          configMap.put("otel.metrics.exporter", "none");
          configMap.put("otel.traces.exporter", "logging");
          configMap.put("otel.logs.exporter", "logging");
          return configMap;
        })
        .addSpanExporterCustomizer((exporter,c)->spansExporter)
        .addLogRecordExporterCustomizer(
            (logRecordExporter, configProperties) -> memoryLogRecordExporter)
        .addLoggerProviderCustomizer(
            new BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder>() {
              @Override
              public SdkLoggerProviderBuilder apply(
                  SdkLoggerProviderBuilder sdkLoggerProviderBuilder,
                  ConfigProperties configProperties) {
                return sdkLoggerProviderBuilder.addLogRecordProcessor(new FilteringLogRecordProcessor(
                    logRecordProcessor, logRecordData -> logRecordData.getSpanContext().isSampled()));
              }
            });

    logger =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(new FilteringLogRecordProcessor(logRecordProcessor,
                logRecordData -> {
                  SpanContext spanContext =logRecordData.getSpanContext();
                  return spanContext.isSampled();
                }) {})
            .build()
            .get("TestScope");
  }

  @Test
  void verifyLogFilteringExistSpanContext() {

    try (OpenTelemetrySdk sdk = sdkBuilder.build().getOpenTelemetrySdk()) {
      Tracer tracer = sdk.getTracer("test");
      Span span = tracer.spanBuilder("test").startSpan();
      sdk.getLogsBridge().get("test").logRecordBuilder().setBody("One Log").emit();
      List<LogRecordData>  finishedLogRecordItems = memoryLogRecordExporter.getFinishedLogRecordItems();
      assertEquals(1, finishedLogRecordItems.size());
      try (Scope scope = span.makeCurrent()) {

      } finally {
        span.end();
      }
      List<SpanData> finishedSpans = spansExporter.getFinishedSpanItems();
      assertEquals(1, finishedSpans.size());
    }
  }

  @Test
  void verifyFilteringNotExitSpanContext() {
    logger.logRecordBuilder().setBody("One Log").emit();
    List<LogRecordData>  finishedLogRecordItems = memoryLogRecordExporter.getFinishedLogRecordItems();
    assertEquals(0,finishedLogRecordItems.size());

  }


}
