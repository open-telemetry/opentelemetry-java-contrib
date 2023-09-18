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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Utility class to simplify tracing.
 *
 * <p>The <a
 * href="https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/extended-tracer/README.md">README</a>
 * explains the use cases in more detail.
 */
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

  /**
   * Creates a new instance of {@link Tracing}.
   *
   * @param openTelemetry the {@link OpenTelemetry} instance
   * @param tracerName the name of the tracer to use
   */
  public Tracing(OpenTelemetry openTelemetry, String tracerName) {
    this(openTelemetry, openTelemetry.getTracer(tracerName));
  }

  /**
   * Creates a new instance of {@link Tracing}.
   *
   * @param openTelemetry the {@link OpenTelemetry} instance
   * @param tracer the {@link Tracer} to use
   */
  public Tracing(OpenTelemetry openTelemetry, Tracer tracer) {
    this.openTelemetry = openTelemetry;
    this.tracer = tracer;
  }

  /**
   * Creates a new {@link SpanBuilder} with the given span name.
   *
   * @param spanName the name of the span
   * @return the {@link SpanBuilder}
   */
  public SpanBuilder spanBuilder(String spanName) {
    return tracer.spanBuilder(spanName);
  }

  /**
   * Runs the given {@link Runnable} in a new span with the given name.
   *
   * <p>If an exception is thrown by the {@link Runnable}, the span will be marked as error, and the
   * exception will be recorded.
   *
   * @param spanName the name of the span
   * @param runnable the {@link Runnable} to run
   */
  public void run(String spanName, Runnable runnable) {
    run(tracer.spanBuilder(spanName), runnable);
  }

  /**
   * Runs the given {@link Runnable} inside of the span created by the given {@link SpanBuilder}.
   * The span will be ended at the end of the {@link Runnable}.
   *
   * <p>If an exception is thrown by the {@link Runnable}, the span will be marked as error, and the
   * exception will be recorded.
   *
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param runnable the {@link Runnable} to run
   */
  @SuppressWarnings("NullAway")
  public void run(SpanBuilder spanBuilder, Runnable runnable) {
    call(
        spanBuilder,
        () -> {
          runnable.run();
          return null;
        });
  }

  /**
   * Runs the given {@link ThrowingSupplier} inside a new span with the given name.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the span will be marked as error,
   * and the exception will be recorded.
   *
   * @param spanName the name of the span
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T call(String spanName, ThrowingSupplier<T, E> throwingSupplier)
      throws E {
    return call(tracer.spanBuilder(spanName), throwingSupplier);
  }

  /**
   * Runs the given {@link ThrowingSupplier} inside of the span created by the given {@link
   * SpanBuilder}. The span will be ended at the end of the {@link ThrowingSupplier}.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the span will be marked as error,
   * and the exception will be recorded.
   *
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T call(
      SpanBuilder spanBuilder, ThrowingSupplier<T, E> throwingSupplier) throws E {
    return call(spanBuilder, throwingSupplier, Tracing::setSpanError);
  }

  /**
   * Runs the given {@link ThrowingSupplier} inside of the span created by the given {@link
   * SpanBuilder}. The span will be ended at the end of the {@link ThrowingSupplier}.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the handleException consumer will
   * be called, giving you the opportunity to handle the exception and span in a custom way, e.g.
   * not marking the span as error.
   *
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param handleException the consumer to call when an exception is thrown
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  @SuppressWarnings("NullAway")
  public <T, E extends Throwable> T call(
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier,
      BiConsumer<Span, Throwable> handleException)
      throws E {
    Span span = spanBuilder.startSpan();
    //noinspection unused
    try (Scope unused = span.makeCurrent()) {
      return throwingSupplier.get();
    } catch (Throwable e) {
      handleException.accept(span, e);
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Marks a span as error.
   *
   * @param span the span
   * @param exception the exception that caused the error
   */
  private static void setSpanError(Span span, Throwable exception) {
    span.setStatus(StatusCode.ERROR);
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
  private Context extractContext(Map<String, String> transport) {
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

  /**
   * Set baggage items inside the given {@link ThrowingSupplier}.
   *
   * @param baggage the baggage items to set
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  @SuppressWarnings("NullAway")
  public <T, E extends Throwable> T callWithBaggage(
      Map<String, String> baggage, ThrowingSupplier<T, E> throwingSupplier) throws E {
    BaggageBuilder builder = Baggage.current().toBuilder();
    baggage.forEach(builder::put);
    Context context = builder.build().storeInContext(Context.current());
    try (Scope ignore = context.makeCurrent()) {
      return throwingSupplier.get();
    }
  }

  /**
   * Run the given {@link Runnable} inside a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the span will be marked as error,
   * and the exception will be recorded.
   *
   * @param transport the transport where to extract the span context from
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T traceServerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier)
      throws E {
    return extractAndRun(SERVER, transport, spanBuilder, throwingSupplier, Tracing::setSpanError);
  }

  /**
   * Run the given {@link Runnable} inside a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the handleException consumer will
   * be called, giving you the opportunity to handle the exception and span in a custom way, e.g.
   * not marking the span as error.
   *
   * @param transport the transport where to extract the span context from
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param handleException the consumer to call when an exception is thrown
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T traceServerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier,
      BiConsumer<Span, Throwable> handleException)
      throws E {
    return extractAndRun(SERVER, transport, spanBuilder, throwingSupplier, handleException);
  }

  /**
   * Run the given {@link Runnable} inside a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the span will be marked as error,
   * and the exception will be recorded.
   *
   * @param transport the transport where to extract the span context from
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T traceConsumerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier)
      throws E {
    return extractAndRun(CONSUMER, transport, spanBuilder, throwingSupplier, Tracing::setSpanError);
  }

  /**
   * Run the given {@link Runnable} inside a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   *
   * <p>If an exception is thrown by the {@link ThrowingSupplier}, the handleException consumer will
   * be called, giving you the opportunity to handle the exception and span in a custom way, e.g.
   * not marking the span as error.
   *
   * @param transport the transport where to extract the span context from
   * @param spanBuilder the {@link SpanBuilder} to use
   * @param throwingSupplier the {@link ThrowingSupplier} to call
   * @param handleException the consumer to call when an exception is thrown
   * @param <T> the type of the result
   * @param <E> the type of the exception
   * @return the result of the {@link ThrowingSupplier}
   */
  public <T, E extends Throwable> T traceConsumerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier,
      BiConsumer<Span, Throwable> handleException)
      throws E {
    return extractAndRun(CONSUMER, transport, spanBuilder, throwingSupplier, handleException);
  }

  private <T, E extends Throwable> T extractAndRun(
      SpanKind spanKind,
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      ThrowingSupplier<T, E> throwingSupplier,
      BiConsumer<Span, Throwable> handleException)
      throws E {
    try (Scope ignore = extractContext(transport).makeCurrent()) {
      return call(spanBuilder.setSpanKind(spanKind), throwingSupplier, handleException);
    }
  }
}
