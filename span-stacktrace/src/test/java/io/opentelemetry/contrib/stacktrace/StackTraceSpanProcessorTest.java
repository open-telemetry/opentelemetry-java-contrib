/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.stacktrace.util.TestUtils;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackTraceSpanProcessorTest {

  private InMemorySpanExporter spansExporter;
  private SpanProcessor exportProcessor;

  @BeforeEach
  public void setup() {
    spansExporter = InMemorySpanExporter.create();
    exportProcessor = SimpleSpanProcessor.create(spansExporter);
  }

  @Test
  void durationAndFiltering() {
    // over duration threshold
    testSpan(span -> true, 11, 1);
    // under duration threshold
    testSpan(span -> true, 9, 0);

    // filtering out span
    testSpan(span -> false, 20, 0);
  }

  @Test
  void spanWithExistingStackTrace() {
    testSpan(
        span -> true,
        20,
        1,
        sb -> sb.setAttribute(StackTraceSpanProcessor.SPAN_STACKTRACE, "hello"),
        stacktrace -> assertThat(stacktrace).isEqualTo("hello"));
  }

  private void testSpan(
      Function<ReadableSpan, Boolean> includeFilter,
      long spanDurationNanos,
      int expectedSpansCount) {
    testSpan(
        includeFilter,
        spanDurationNanos,
        expectedSpansCount,
        Function.identity(),
        (stackTrace) ->
            assertThat(stackTrace)
                .describedAs("span stack trace should contain caller class name")
                .contains(StackTraceSpanProcessorTest.class.getCanonicalName()));
  }

  private void testSpan(
      Function<ReadableSpan, Boolean> includeFilter,
      long spanDurationNanos,
      int expectedSpansCount,
      Function<SpanBuilder, SpanBuilder> customizeSpanBuilder,
      Consumer<String> stackTraceCheck) {
    try (SpanProcessor processor =
        new StackTraceSpanProcessor(exportProcessor, 10, includeFilter)) {

      OpenTelemetrySdk sdk = TestUtils.sdkWith(processor);
      Tracer tracer = sdk.getTracer("test");

      Instant start = Instant.now();
      Instant end = start.plusNanos(spanDurationNanos);
      Span span =
          customizeSpanBuilder
              .apply(tracer.spanBuilder("span").setStartTimestamp(start))
              .startSpan();
      try (Scope scope = span.makeCurrent()) {
      } finally {
        span.end(end);
      }

      List<SpanData> finishedSpans = spansExporter.getFinishedSpanItems();
      assertThat(finishedSpans).hasSize(expectedSpansCount);

      if (!finishedSpans.isEmpty()) {
        String stackTrace =
            finishedSpans.get(0).getAttributes().get(StackTraceSpanProcessor.SPAN_STACKTRACE);

        stackTraceCheck.accept(stackTrace);
      }
    }
  }
}
