/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import static java.util.Collections.singletonList;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaggageSpanProcessorTest {

  @Test
  void test_baggageSpanProcessor_adds_attributes_to_spans(@Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor = new BaggageSpanProcessor(null, null)) {
      try (Scope ignore = Baggage.current().toBuilder().put("key", "value").build().makeCurrent()) {
        processor.onStart(Context.current(), span);
        Mockito.verify(span).setAttribute("key", "value");
      }
    }
  }

  @Test
  void test_baggageSpanProcessor_adds_attributes_to_spans_when_key_filter_matches(
      @Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor = new BaggageSpanProcessor(singletonList("k*"), null)) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("other", "value")
              .build()
              .makeCurrent()) {
        processor.onStart(Context.current(), span);
        Mockito.verify(span).setAttribute("key", "value");
        Mockito.verify(span, Mockito.never()).setAttribute("other", "value");
      }
    }
  }

  @Test
  void test_baggageSpanProcessor_adds_attributes_to_spans_include_exclude(
      @Mock ReadWriteSpan span) {
    try (BaggageSpanProcessor processor =
        new BaggageSpanProcessor(singletonList("k*"), singletonList("*ignored"))) {
      try (Scope ignore =
          Baggage.current().toBuilder()
              .put("key", "value")
              .put("key_is_ignored", "value")
              .build()
              .makeCurrent()) {
        processor.onStart(Context.current(), span);
        Mockito.verify(span).setAttribute("key", "value");
        Mockito.verify(span, Mockito.never()).setAttribute("key_is_ignored", "value");
      }
    }
  }
}
