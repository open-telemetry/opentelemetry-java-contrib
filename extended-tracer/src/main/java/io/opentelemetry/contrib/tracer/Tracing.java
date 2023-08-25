/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.tracer;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class Tracing {

  private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER =
      new TextMapGetter<Map<String, String>>() {
        @Override
        public Set<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Override
        @Nullable
        public String get(@Nullable Map<String, String> carrier, String key) {
          //noinspection ConstantConditions
          return carrier == null ? null : carrier.get(key);
        }
      };
  private final OpenTelemetry openTelemetry;

  private final Tracer tracer;

  public Tracing(OpenTelemetry openTelemetry, String tracerName) {
    this.openTelemetry = openTelemetry;
    this.tracer = openTelemetry.getTracer(tracerName);
  }

  public void run(String spanName, Runnable runnable) {
    runAndEndSpan(tracer.spanBuilder(spanName).startSpan(), runnable);
  }

  public void runAndEndSpan(Span span, Runnable runnable) {
    callAndEndSpan(
        span,
        () -> {
          runnable.run();
          return null;
        });
  }

  /**
   * Runs a block of code with a new span - ending the span at the end and recording any exception.
   *
   * @param spanName name of the new span
   */
  public <T> T call(String spanName, Callable<T> callable) {
    return callAndEndSpan(tracer.spanBuilder(spanName).startSpan(), callable);
  }

  public <T> T callAndEndSpan(Span span, Callable<T> callable) {
    return callAndEndSpan(span, callable, this::setSpanError);
  }

  @SuppressWarnings("NullAway")
  public <T> T callAndEndSpan(
      Span span, Callable<T> callable, BiConsumer<Span, Exception> handleException) {
    //noinspection unused
    try (Scope scope = span.makeCurrent()) {
      return callable.call();
    } catch (Exception e) {
      handleException.accept(span, e);
      sneakyThrow(e);
      return null;
    } finally {
      span.end();
    }
  }

  /**
   * Marks a span as error.
   *
   * @param span the span
   * @param description what went wrong
   */
  public void setSpanError(Span span, String description) {
    span.setStatus(StatusCode.ERROR, description);
  }

  /**
   * Marks a span as error.
   *
   * @param span the span
   * @param exception the exception that caused the error
   */
  public void setSpanError(Span span, Throwable exception) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(exception);
  }

  /**
   * Marks a span as error.
   *
   * @param span the span
   * @param description what went wrong
   * @param exception the exception that caused the error
   */
  public void setSpanError(Span span, String description, Throwable exception) {
    span.setStatus(StatusCode.ERROR, description);
    span.recordException(exception);
  }

  /**
   * Injects the current context into a string map, which can then be added to HTTP headers or the
   * metadata of an event.
   */
  public Map<String, String> getPropagationHeaders() {
    Map<String, String> transport = new HashMap<>();
    //noinspection ConstantConditions
    openTelemetry
        .getPropagators()
        .getTextMapPropagator()
        .inject(
            Context.current(),
            transport,
            (map, key, value) -> {
              if (map != null) {
                map.put(key, value);
              }
            });

    return transport;
  }

  /**
   * Extract the context from a string map, which you get from HTTP headers of the metadata of an
   * event you're processing.
   */
  public Context extractContext(Map<String, String> transport) {
    Context current = Context.current();
    //noinspection ConstantConditions
    if (transport == null) {
      return current;
    }
    // HTTP headers are case-insensitive. As we're using Map, which is case-sensitive, we need to
    // normalize all the keys
    Map<String, String> normalizedTransport =
        transport.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().toLowerCase(Locale.ROOT), Map.Entry::getValue));
    return openTelemetry
        .getPropagators()
        .getTextMapPropagator()
        .extract(current, normalizedTransport, TEXT_MAP_GETTER);
  }

  /** Sets baggage items which are active in given block. */
  @SuppressWarnings("NullAway")
  public <T> T callWithBaggage(Map<String, String> baggage, Callable<T> callable) {
    BaggageBuilder builder = Baggage.current().toBuilder();
    baggage.forEach(builder::put);
    Context context = builder.build().storeInContext(Context.current());
    try (Scope ignore = context.makeCurrent()) {
      return callable.call();
    } catch (Throwable e) {
      sneakyThrow(e);
      return null;
    }
  }

  /**
   * Trace a block of code using a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceServerSpan(
      Map<String, String> transport, SpanBuilder spanBuilder, Callable<T> callable) {
    return extractAndRun(SERVER, transport, spanBuilder, callable, this::setSpanError);
  }

  /**
   * Trace a block of code using a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceServerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      Callable<T> callable,
      BiConsumer<Span, Exception> handleException) {
    return extractAndRun(SERVER, transport, spanBuilder, callable, handleException);
  }

  /**
   * Trace a block of code using a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceConsumerSpan(
      Map<String, String> transport, SpanBuilder spanBuilder, Callable<T> callable) {
    return extractAndRun(CONSUMER, transport, spanBuilder, callable, this::setSpanError);
  }

  /**
   * Trace a block of code using a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceConsumerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      Callable<T> callable,
      BiConsumer<Span, Exception> handleException) {
    return extractAndRun(CONSUMER, transport, spanBuilder, callable, handleException);
  }

  private <T> T extractAndRun(
      SpanKind spanKind,
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      Callable<T> callable,
      BiConsumer<Span, Exception> handleException) {
    try (Scope ignore = extractContext(transport).makeCurrent()) {
      return callAndEndSpan(
          spanBuilder.setSpanKind(spanKind).startSpan(), callable, handleException);
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }
}
