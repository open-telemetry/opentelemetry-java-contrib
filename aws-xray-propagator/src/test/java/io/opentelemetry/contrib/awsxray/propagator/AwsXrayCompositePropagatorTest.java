/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class AwsXrayCompositePropagatorTest extends AwsXrayPropagatorTest {

  @Override
  TextMapPropagator propagator() {
    return TextMapPropagator.composite(
        W3CBaggagePropagator.getInstance(),
        AwsXrayPropagator.getInstance(),
        W3CTraceContextPropagator.getInstance());
  }

  @Test
  void extract_traceContextOverridesXray() {
    LinkedHashMap<String, String> carrier = new LinkedHashMap<>();
    String w3cTraceContextTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    String w3cTraceContextSpanId = "00f067aa0ba902b7";
    String traceParent =
        String.format("00-%s-%s-01", w3cTraceContextTraceId, w3cTraceContextSpanId);
    String traceState = "rojo=00f067aa0ba902b7";
    String xrayTrace = String.format("Root=1-%s;Parent=%s;Sampled=0", TRACE_ID, SPAN_ID);

    carrier.put("traceparent", traceParent);
    carrier.put("tracestate", traceState);
    carrier.put("X-Amzn-Trace-Id", xrayTrace);

    SpanContext actualContext = getSpanContext(subject.extract(Context.current(), carrier, GETTER));

    assertThat(actualContext.getTraceId()).isEqualTo(w3cTraceContextTraceId);
    assertThat(actualContext.getSpanId()).isEqualTo(w3cTraceContextSpanId);
    assertThat(actualContext.isSampled()).isEqualTo(true);
  }

  @Test
  void extract_xrayOverridesTraceContext() {
    TextMapPropagator propagator =
        TextMapPropagator.composite(
            W3CBaggagePropagator.getInstance(),
            W3CTraceContextPropagator.getInstance(),
            AwsXrayPropagator.getInstance());

    LinkedHashMap<String, String> carrier = new LinkedHashMap<>();
    String w3cTraceContextTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    String w3cTraceContextSpanId = "00f067aa0ba902b7";
    String traceParent =
        String.format("00-%s-%s-01", w3cTraceContextTraceId, w3cTraceContextSpanId);
    String traceState = "rojo=00f067aa0ba902b7";
    String xrayTrace =
        String.format(
            "Root=1-%s;Parent=%s;Sampled=0", "8a3c60f7-d188f8fa79d48a391a778fa6", SPAN_ID);

    carrier.put("traceparent", traceParent);
    carrier.put("tracestate", traceState);
    carrier.put("X-Amzn-Trace-Id", xrayTrace);

    SpanContext actualContext =
        getSpanContext(propagator.extract(Context.current(), carrier, GETTER));

    assertThat(actualContext.getTraceId()).isEqualTo(TRACE_ID);
    assertThat(actualContext.getSpanId()).isEqualTo(SPAN_ID);
    assertThat(actualContext.isSampled()).isEqualTo(false);
  }
}
