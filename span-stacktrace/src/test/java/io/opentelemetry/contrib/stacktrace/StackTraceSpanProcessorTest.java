/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
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
  void tryInvalidMinDuration() {
    assertThatCode(() -> new StackTraceSpanProcessor(-1, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void durationAndFiltering() {
    // on duration threshold
    checkSpanWithStackTrace("1ms", msToNs(1));
    // over duration threshold
    checkSpanWithStackTrace("1ms", msToNs(2));
    // under duration threshold
    checkSpanWithoutStackTrace(YesPredicate.class, "2ms", msToNs(1));

    // filtering out span
    checkSpanWithoutStackTrace(NoPredicate.class, "1ms", msToNs(20));
  }

  public static class YesPredicate implements Predicate<ReadableSpan> {

    @Override
    public boolean test(ReadableSpan readableSpan) {
      return true;
    }
  }

  public static class NoPredicate implements Predicate<ReadableSpan> {
    @Override
    public boolean test(ReadableSpan readableSpan) {
      return false;
    }
  }

  @Test
  void defaultConfig() {
    long expectedDefault = msToNs(5);
    checkSpanWithStackTrace(null, expectedDefault);
    checkSpanWithStackTrace(null, expectedDefault + 1);
    checkSpanWithoutStackTrace(YesPredicate.class, null, expectedDefault - 1);
  }

  @Test
  void disabledConfig() {
    checkSpanWithoutStackTrace(YesPredicate.class, "-1", 5);
  }

  @Test
  void spanWithExistingStackTrace() {
    checkSpan(
        YesPredicate.class,
        "1ms",
        Duration.ofMillis(1).toNanos(),
        sb -> sb.setAttribute(CodeIncubatingAttributes.CODE_STACKTRACE, "hello"),
        stacktrace -> assertThat(stacktrace).isEqualTo("hello"));
  }

  private static void checkSpanWithStackTrace(String minDurationString, long spanDurationNanos) {
    checkSpan(
        YesPredicate.class,
        minDurationString,
        spanDurationNanos,
        Function.identity(),
        (stackTrace) ->
            assertThat(stackTrace)
                .describedAs("span stack trace should contain caller class name")
                .contains(StackTraceSpanProcessorTest.class.getCanonicalName()));
  }

  private static void checkSpanWithoutStackTrace(
      Class<? extends Predicate<?>> predicateClass,
      String minDurationString,
      long spanDurationNanos) {
    checkSpan(
        predicateClass,
        minDurationString,
        spanDurationNanos,
        Function.identity(),
        (stackTrace) -> assertThat(stackTrace).describedAs("no stack trace expected").isNull());
  }

  private static void checkSpan(
      Class<? extends Predicate<?>> predicateClass,
      String minDurationString,
      long spanDurationNanos,
      Function<SpanBuilder, SpanBuilder> customizeSpanBuilder,
      Consumer<String> stackTraceCheck) {

    // must be re-created on every test as exporter is shut down on span processor close
    InMemorySpanExporter spansExporter = InMemorySpanExporter.create();

    AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
    sdkBuilder.addPropertiesSupplier(
        () -> {
          Map<String, String> configMap = new HashMap<>();

          configMap.put("otel.metrics.exporter", "none");
          configMap.put("otel.traces.exporter", "logging");
          configMap.put("otel.logs.exporter", "none");

          if (minDurationString != null) {
            configMap.put("otel.java.experimental.span-stacktrace.min.duration", minDurationString);
          }
          if (predicateClass != null) {
            configMap.put(
                "otel.java.experimental.span-stacktrace.filter", predicateClass.getName());
          }
          return configMap;
        });
    // duplicate export to our in-memory span exporter
    sdkBuilder.addSpanExporterCustomizer(
        (exporter, config) -> SpanExporter.composite(exporter, spansExporter));

    new StackTraceAutoConfig().customize(sdkBuilder);

    try (OpenTelemetrySdk sdk = sdkBuilder.build().getOpenTelemetrySdk()) {

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
