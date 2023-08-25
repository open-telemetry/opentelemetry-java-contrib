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
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TracingTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private final Tracing tracing = new Tracing(otelTesting.getOpenTelemetry(), "test");

  private final Tracer tracer = otelTesting.getOpenTelemetry().getTracer("test");

  @Test
  void propagation() {
    tracing.run(
        "parent",
        () -> {
          Map<String, String> propagationHeaders = tracing.getPropagationHeaders();
          assertThat(propagationHeaders).hasSize(1).containsKey("traceparent");

          assertThat(
              Span.fromContext(tracing.extractContext(propagationHeaders))
                  .getSpanContext()
                  .getSpanId())
              .isEqualTo(Span.current().getSpanContext().getSpanId());

          tracing.traceServerSpan(
              propagationHeaders, tracer.spanBuilder("child"), () -> null);
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
    private final BiConsumer<TracingTest, Callable<Void>> extractAndRun;
    private final SpanKind wantKind;
    private final StatusData wantStatus;

    private ExtractAndRunParameter(
        BiConsumer<TracingTest, Callable<Void>> extractAndRun, SpanKind wantKind, StatusData wantStatus) {
      this.extractAndRun = extractAndRun;
      this.wantKind = wantKind;
      this.wantStatus = wantStatus;
    }
  }

  @Keep
  private static Stream<Arguments> extractAndRun() {
    BiConsumer<Span, Exception> ignoreException =
        (span, throwable) -> {
          // ignore
        };
    return Stream.of(
        Arguments.of(
            named(
                "server",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.tracing.traceServerSpan(
                            Collections.emptyMap(), t.tracer.spanBuilder("span"), c),
                    io.opentelemetry.api.trace.SpanKind.SERVER,
                    io.opentelemetry.sdk.trace.data.StatusData.error()))),
        Arguments.of(
            named(
                "server - ignore exception",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.tracing.traceServerSpan(
                            Collections.emptyMap(),
                            t.tracer.spanBuilder("span"),
                            c,
                            ignoreException),
                    io.opentelemetry.api.trace.SpanKind.SERVER,
                    io.opentelemetry.sdk.trace.data.StatusData.unset()))),
        Arguments.of(
            named(
                "consumer",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.tracing.traceConsumerSpan(
                            Collections.emptyMap(), t.tracer.spanBuilder("span"), c),
                    io.opentelemetry.api.trace.SpanKind.CONSUMER,
                    io.opentelemetry.sdk.trace.data.StatusData.error()))),
        Arguments.of(
            named(
                "consumer - ignore exception",
                new ExtractAndRunParameter(
                    (t, c) ->
                        t.tracing.traceConsumerSpan(
                            Collections.emptyMap(),
                            t.tracer.spanBuilder("span"),
                            c,
                            ignoreException),
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
                    this,
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

  private static class SetSpanErrorParameter {
    private final Consumer<Span> setError;
    private final String wantDescription;
    private final Throwable wantException;

    private SetSpanErrorParameter(
        Consumer<Span> setError, String wantDescription, Throwable wantException) {
      this.setError = setError;
      this.wantDescription = wantDescription;
      this.wantException = wantException;
    }
  }

  @Keep
  private static Stream<Arguments> setSpanError() {
    Tracing tracing = new Tracing(otelTesting.getOpenTelemetry(), "test");
    RuntimeException exception = new RuntimeException("ex");
    return Stream.of(
        Arguments.of(
            named(
                "with description",
                new SetSpanErrorParameter(s -> tracing.setSpanError(s, "error"), "error", null))),
        Arguments.of(
            named(
                "with exception",
                new SetSpanErrorParameter(
                    s -> tracing.setSpanError(s, exception), null, exception))),
        Arguments.of(
            named(
                "with exception and description",
                new SetSpanErrorParameter(
                    s -> tracing.setSpanError(s, "error", exception), "error", exception))));
  }

  @ParameterizedTest
  @MethodSource
  void setSpanError(SetSpanErrorParameter parameter) {
    tracing.run("parent", () -> parameter.setError.accept(Span.current()));

    otelTesting
        .assertTraces()
        .hasSize(1)
        .hasTracesSatisfyingExactly(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> {
                      SpanDataAssert spanDataAssert =
                          span.hasStatus(
                              StatusData.create(StatusCode.ERROR, parameter.wantDescription));
                      if (parameter.wantException != null) {
                        spanDataAssert.hasEventsSatisfyingExactly(
                            event ->
                                event.hasAttributesSatisfyingExactly(
                                    OpenTelemetryAssertions.satisfies(
                                        SemanticAttributes.EXCEPTION_TYPE,
                                        s ->
                                            s.isEqualTo(
                                                parameter.wantException.getClass().getName())),
                                    OpenTelemetryAssertions.satisfies(
                                        SemanticAttributes.EXCEPTION_MESSAGE,
                                        AbstractCharSequenceAssert::isNotBlank),
                                    OpenTelemetryAssertions.satisfies(
                                        SemanticAttributes.EXCEPTION_STACKTRACE,
                                        AbstractCharSequenceAssert::isNotBlank)));
                      } else {
                        spanDataAssert.hasTotalRecordedEvents(0);
                      }
                    }));
  }
}
