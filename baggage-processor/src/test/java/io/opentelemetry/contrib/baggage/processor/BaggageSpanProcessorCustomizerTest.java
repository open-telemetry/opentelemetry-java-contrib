/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.internal.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaggageSpanProcessorCustomizerTest {

  private static final String MEMORY_EXPORTER = "memory";

  @Test
  void test_customizer() {
    assertCustomizer(Collections.emptyMap(), span -> span.hasTotalAttributeCount(0));
    assertCustomizer(
        Collections.singletonMap(
            "otel.java.experimental.span-attributes.copy-from-baggage.include", "key"),
        span -> span.hasAttribute(AttributeKey.stringKey("key"), "value"));
  }

  private static void assertCustomizer(
      Map<String, String> properties, Consumer<SpanDataAssert> spanDataAssertConsumer) {

    InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

    OpenTelemetrySdk sdk = getOpenTelemetrySdk(properties, spanExporter);
    try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
      sdk.getTracer("test").spanBuilder("test").startSpan().end();
    }
    await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () ->
                TracesAssert.assertThat(spanExporter.getFinishedSpanItems())
                    .hasTracesSatisfyingExactly(
                        trace -> trace.hasSpansSatisfyingExactly(spanDataAssertConsumer)));
  }

  private static OpenTelemetrySdk getOpenTelemetrySdk(
      Map<String, String> properties, InMemorySpanExporter spanExporter) {
    SpiHelper spiHelper =
        SpiHelper.create(BaggageSpanProcessorCustomizerTest.class.getClassLoader());

    AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () ->
                    ImmutableMap.of(
                        // We set the export interval of the spans to 100 ms. The default value is 5
                        // seconds.
                        "otel.bsp.schedule.delay",
                        "10",
                        "otel.traces.exporter",
                        MEMORY_EXPORTER,
                        "otel.metrics.exporter",
                        "none",
                        "otel.logs.exporter",
                        "none"))
            .addPropertiesSupplier(() -> properties);
    AutoConfigureUtil.setComponentLoader(
        sdkBuilder,
        new ComponentLoader() {
          @SuppressWarnings("unchecked")
          @Override
          public <T> List<T> load(Class<T> spiClass) {
            if (spiClass == ConfigurableSpanExporterProvider.class) {
              return Collections.singletonList(
                  (T)
                      new ConfigurableSpanExporterProvider() {
                        @Override
                        public SpanExporter createExporter(ConfigProperties configProperties) {
                          return spanExporter;
                        }

                        @Override
                        public String getName() {
                          return MEMORY_EXPORTER;
                        }
                      });
            }
            return spiHelper.load(spiClass);
          }
        });
    return sdkBuilder.build().getOpenTelemetrySdk();
  }

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans(@Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor =
        BaggageSpanProcessorCustomizer.createProcessor(Collections.singletonList("*"))) {
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
        BaggageSpanProcessorCustomizer.createProcessor(Collections.singletonList("key"))) {
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
}
