/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BaggageSpanProcessorTest {

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans(@Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor = BaggageSpanProcessor.allowAllBaggageKeys()) {
      try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
        processor.onEnding(span);
        Mockito.verify(span).setAttribute("key", "value");
      }
    }
  }

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans_when_key_filter_matches(
      @Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor = new BaggageSpanProcessor(key -> key.startsWith("k"))) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("other", "value")
              .build()
              .makeCurrent()) {
        processor.onEnding(span);
        Mockito.verify(span).setAttribute("key", "value");
        Mockito.verify(span, Mockito.never()).setAttribute("other", "value");
      }
    }
  }

  @Test
  public void test_baggageSpanProcessor_adds_attributes_to_spans_when_key_filter_matches_regex(
      @Mock ReadWriteSpan span) {
    Pattern pattern = Pattern.compile("k.*");
    try (BaggageSpanProcessor processor =
        new BaggageSpanProcessor(key -> pattern.matcher(key).matches())) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("other", "value")
              .build()
              .makeCurrent()) {
        processor.onEnding(span);
        Mockito.verify(span).setAttribute("key", "value");
        Mockito.verify(span, Mockito.never()).setAttribute("other", "value");
      }
    }
  }
}
