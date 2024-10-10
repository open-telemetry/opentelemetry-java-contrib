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
import io.opentelemetry.contrib.stacktrace.internal.TestUtils;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class StackTraceSpanProcessorTest {

  private static long msToNs(int ms) {
    return Duration.ofMillis(ms).toNanos();
  }

  @Test
  void durationAndFiltering() {
    // on duration threshold
    checkSpanWithStackTrace(span -> true, "1ms", msToNs(1));
    // over duration threshold
    checkSpanWithStackTrace(span -> true, "1ms", msToNs(2));
    // under duration threshold
    checkSpanWithoutStackTrace(span -> true, "2ms", msToNs(1));

    // filtering out span
    checkSpanWithoutStackTrace(span -> false, "1ms", 20);
  }

  @Test
  void defaultConfig() {
    long expectedDefault = msToNs(5);
    checkSpanWithStackTrace(span -> true, null, expectedDefault);
    checkSpanWithStackTrace(span -> true, null, expectedDefault + 1);
    checkSpanWithoutStackTrace(span -> true, null, expectedDefault - 1);
  }

  @Test
  void disabledConfig() {
    checkSpanWithoutStackTrace(span -> true, "-1", 5);
  }

  @Test
  void spanWithExistingStackTrace() {
    checkSpan(
        span -> true,
        "1ms",
        Duration.ofMillis(1).toNanos(),
        sb -> sb.setAttribute(CodeIncubatingAttributes.CODE_STACKTRACE, "hello"),
        stacktrace -> assertThat(stacktrace).isEqualTo("hello"));
  }

  private static void checkSpanWithStackTrace(
      Predicate<ReadableSpan> filterPredicate, String configString, long spanDurationNanos) {
    checkSpan(
        filterPredicate,
        configString,
        spanDurationNanos,
        Function.identity(),
        (stackTrace) ->
            assertThat(stackTrace)
                .describedAs("span stack trace should contain caller class name")
                .contains(StackTraceSpanProcessorTest.class.getCanonicalName()));
  }

  private static void checkSpanWithoutStackTrace(
      Predicate<ReadableSpan> filterPredicate, String configString, long spanDurationNanos) {
    checkSpan(
        filterPredicate,
        configString,
        spanDurationNanos,
        Function.identity(),
        (stackTrace) -> assertThat(stackTrace).describedAs("no stack trace expected").isNull());
  }

  private static void checkSpan(
      Predicate<ReadableSpan> filterPredicate,
      String configString,
      long spanDurationNanos,
      Function<SpanBuilder, SpanBuilder> customizeSpanBuilder,
      Consumer<String> stackTraceCheck) {

    // they must be re-created as they are shutdown when the span processor is closed
    InMemorySpanExporter spansExporter = InMemorySpanExporter.create();
    SpanProcessor exportProcessor = SimpleSpanProcessor.create(spansExporter);

    Map<String, String> configMap = new HashMap<>();
    if (configString != null) {
      configMap.put("otel.java.experimental.span-stacktrace.min.duration", configString);
    }

    try (SpanProcessor processor =
        new StackTraceSpanProcessor(
            exportProcessor, DefaultConfigProperties.createFromMap(configMap), filterPredicate)) {

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
      assertThat(finishedSpans).hasSize(1);

      String stackTrace =
          finishedSpans.get(0).getAttributes().get(CodeIncubatingAttributes.CODE_STACKTRACE);

      stackTraceCheck.accept(stackTrace);
    }
  }
}
