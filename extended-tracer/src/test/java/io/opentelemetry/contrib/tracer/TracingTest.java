/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.junit.jupiter.api.Named.named;

import com.google.errorprone.annotations.Keep;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TracingTest {

  interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws Throwable;
  }

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private final Tracing tracing = new Tracing(otelTesting.getOpenTelemetry(), "test");

  @Test
  void propagation() {
    tracing.run(
        "parent",
        () -> {
          Map<String, String> propagationHeaders = tracing.getTextMapPropagationContext();
          assertThat(propagationHeaders).hasSize(1).containsKey("traceparent");

          tracing.traceServerSpan(propagationHeaders, tracing.spanBuilder("child"), () -> null);
        });

    otelTesting
        .assertTraces()
        .hasSize(1)
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    SpanDataAssert::hasNoParent, span -> span.hasParent(trace.getSpan(0))));
  }

  @Test
  void callWithBaggage() {
    String value =
        tracing.call(
            "parent",
            () ->
                tracing.callWithBaggage(
                    Collections.singletonMap("key", "value"),
                    () -> Baggage.current().getEntryValue("key")));

    assertThat(value).isEqualTo("value");
  }

  private static class ExtractAndRunParameter {
    private final ThrowingBiConsumer<Tracing, SpanCallback<Void, Throwable>> extractAndRun;
    private final SpanKind wantKind;
    private final StatusData wantStatus;

    private ExtractAndRunParameter(
        ThrowingBiConsumer<Tracing, SpanCallback<Void, Throwable>> extractAndRun,
        SpanKind wantKind,
        StatusData wantStatus) {
      this.extractAndRun = extractAndRun;
      this.wantKind = wantKind;
      this.wantStatus = wantStatus;
    }
  }

  @Keep
  private static Stream<Arguments> extractAndRun() {
    BiConsumer<Span, Throwable> ignoreException =
        (span, throwable) -> {
          // ignore
        };
    return Stream.of(
        Arguments.of(
            named(
                "server",
                new ExtractAndRunParameter(
                    (t, c) -> t.traceServerSpan(Collections.emptyMap(), t.spanBuilder("span"), c),
                    io.opentelemetry.api.trace.SpanKind.SERVER,
                    io.opentelemetry.sdk.trace.data.StatusData.error()))),
        Arguments.of(
            named(
                "server - ignore exception",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.traceServerSpan(
                            Collections.emptyMap(), t.spanBuilder("span"), c, ignoreException),
                    io.opentelemetry.api.trace.SpanKind.SERVER,
                    io.opentelemetry.sdk.trace.data.StatusData.unset()))),
        Arguments.of(
            named(
                "consumer",
                new ExtractAndRunParameter(
                    (t, c) -> t.traceConsumerSpan(Collections.emptyMap(), t.spanBuilder("span"), c),
                    io.opentelemetry.api.trace.SpanKind.CONSUMER,
                    io.opentelemetry.sdk.trace.data.StatusData.error()))),
        Arguments.of(
            named(
                "consumer - ignore exception",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.traceConsumerSpan(
                            Collections.emptyMap(), t.spanBuilder("span"), c, ignoreException),
                    io.opentelemetry.api.trace.SpanKind.CONSUMER,
                    io.opentelemetry.sdk.trace.data.StatusData.unset()))));
  }

  @ParameterizedTest
  @MethodSource
  void extractAndRun(ExtractAndRunParameter parameter) {
    assertThatException()
        .isThrownBy(
            () ->
                parameter.extractAndRun.accept(
                    tracing,
                    () -> {
                      throw new RuntimeException("ex");
                    }));

    otelTesting
        .assertTraces()
        .hasSize(1)
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasKind(parameter.wantKind).hasStatus(parameter.wantStatus)));
  }
}
