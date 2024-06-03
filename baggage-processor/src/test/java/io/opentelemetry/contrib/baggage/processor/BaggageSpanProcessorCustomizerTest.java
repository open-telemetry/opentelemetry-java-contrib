/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaggageSpanProcessorCustomizerTest {

  @Test
  void test_customizer() {
    assertCustomizer(Collections.emptyMap(), 0);
    assertCustomizer(
        Collections.singletonMap(
            "otel.java.experimental.span-attributes.copy-from-baggage.include", "key"),
        1);
  }

  private static void assertCustomizer(
      Map<String, String> properties, int addedSpanProcessorTimes) {
    AutoConfigurationCustomizer customizer = Mockito.mock(AutoConfigurationCustomizer.class);
    new BaggageSpanProcessorCustomizer().customize(customizer);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>>
        captor = ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer, times(1)).addTracerProviderCustomizer(captor.capture());

    SdkTracerProviderBuilder builder = Mockito.mock(SdkTracerProviderBuilder.class);
    SdkTracerProviderBuilder apply =
        captor.getValue().apply(builder, DefaultConfigProperties.createFromMap(properties));
    assertThat(apply).isSameAs(builder);

    verify(builder, times(addedSpanProcessorTimes)).addSpanProcessor(Mockito.any());
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
