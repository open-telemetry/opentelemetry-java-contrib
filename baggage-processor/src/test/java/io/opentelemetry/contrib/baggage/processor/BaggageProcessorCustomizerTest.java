/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaggageProcessorCustomizerTest {

  private static final String MEMORY_EXPORTER = "memory";

  @Test
  void test_customizer() {
    assertCustomizer(
        Collections.emptyMap(),
        span -> assertThat(span).hasTotalAttributeCount(0),
        logRecord -> assertThat(logRecord).hasTotalAttributeCount(0));
    Map<String, String> properties = new HashMap<>();
    properties.put("otel.java.experimental.span-attributes.copy-from-baggage.include", "key");
    properties.put("otel.java.experimental.log-attributes.copy-from-baggage.include", "key");
    assertCustomizer(
        properties,
        span -> assertThat(span.getAttributes()).containsEntry("key", "value"),
        logRecord -> assertThat(logRecord.getAttributes()).containsEntry("key", "value"));
  }

  private static void assertCustomizer(
      Map<String, String> properties,
      Consumer<SpanData> spanDataRequirements,
      Consumer<LogRecordData> logRecordRequirements) {

    InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();

    OpenTelemetrySdk sdk = getOpenTelemetrySdk(properties, spanExporter, logExporter);
    try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
      sdk.getTracer("test").spanBuilder("test").startSpan().end();
      sdk.getLogsBridge().get("test").logRecordBuilder().setBody("test").emit();
    }

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(spanExporter.getFinishedSpanItems())
                  .hasSize(1)
                  .allSatisfy(spanDataRequirements);
              assertThat(logExporter.getFinishedLogRecordItems())
                  .hasSize(1)
                  .allSatisfy(logRecordRequirements);
            });
  }

  private static OpenTelemetrySdk getOpenTelemetrySdk(
      Map<String, String> properties,
      InMemorySpanExporter spanExporter,
      InMemoryLogRecordExporter logRecordExporter) {
    SpiHelper spiHelper = SpiHelper.create(BaggageProcessorCustomizerTest.class.getClassLoader());

    AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () ->
                    ImmutableMap.of(
                        // We set the export interval of the spans to 10 ms. The default value is 5
                        // seconds.
                        "otel.bsp.schedule.delay", // span exporter
                        "100",
                        "otel.blrp.schedule.delay", // log exporter
                        "100",
                        "otel.traces.exporter",
                        MEMORY_EXPORTER,
                        "otel.metrics.exporter",
                        "none",
                        "otel.logs.exporter",
                        MEMORY_EXPORTER))
            .addPropertiesSupplier(() -> properties)
            .setComponentLoader(
                new ComponentLoader() {
                  @Override
                  public <T> List<T> load(Class<T> spiClass) {
                    if (spiClass == ConfigurableSpanExporterProvider.class) {
                      return Collections.singletonList(
                          spiClass.cast(
                              new ConfigurableSpanExporterProvider() {
                                @Override
                                public SpanExporter createExporter(
                                    ConfigProperties configProperties) {
                                  return spanExporter;
                                }

                                @Override
                                public String getName() {
                                  return MEMORY_EXPORTER;
                                }
                              }));
                    } else if (spiClass == ConfigurableLogRecordExporterProvider.class) {
                      return Collections.singletonList(
                          spiClass.cast(
                              new ConfigurableLogRecordExporterProvider() {
                                @Override
                                public LogRecordExporter createExporter(
                                    ConfigProperties configProperties) {
                                  return logRecordExporter;
                                }

                                @Override
                                public String getName() {
                                  return MEMORY_EXPORTER;
                                }
                              }));
                    }
                    return spiHelper.load(spiClass);
                  }
                });
    return sdkBuilder.build().getOpenTelemetrySdk();
  }

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans(@Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor =
        BaggageProcessorCustomizer.createBaggageSpanProcessor(Collections.singletonList("*"))) {
      try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
        processor.onStart(Context.current(), span);
        verify(span).setAttribute("key", "value");
      }
    }
  }

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans_when_key_filter_matches(
      @Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor =
        BaggageProcessorCustomizer.createBaggageSpanProcessor(Collections.singletonList("key"))) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("other", "value")
              .build()
              .makeCurrent()) {
        processor.onStart(Context.current(), span);
        verify(span).setAttribute("key", "value");
        verify(span, Mockito.never()).setAttribute("other", "value");
      }
    }
  }

  @Test
  public void test_baggageLogRecordProcessor_adds_attributes_to_logRecord(
      @Mock ReadWriteLogRecord logRecord) {
    try (BaggageLogRecordProcessor processor =
        BaggageProcessorCustomizer.createBaggageLogRecordProcessor(
            Collections.singletonList("*"))) {
      try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
        processor.onEmit(Context.current(), logRecord);
        verify(logRecord).setAttribute(AttributeKey.stringKey("key"), "value");
      }
    }
  }

  @Test
  public void test_baggageLogRecordProcessor_adds_attributes_to_spans_when_key_filter_matches(
      @Mock ReadWriteLogRecord logRecord) {
    try (BaggageLogRecordProcessor processor =
        BaggageProcessorCustomizer.createBaggageLogRecordProcessor(
            Collections.singletonList("key"))) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("other", "value")
              .build()
              .makeCurrent()) {
        processor.onEmit(Context.current(), logRecord);
        verify(logRecord).setAttribute(AttributeKey.stringKey("key"), "value");
        verify(logRecord, Mockito.never()).setAttribute(AttributeKey.stringKey("other"), "value");
      }
    }
  }
}
