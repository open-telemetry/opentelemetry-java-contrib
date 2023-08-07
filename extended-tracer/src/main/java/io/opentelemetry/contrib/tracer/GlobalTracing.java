/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.tracer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public class GlobalTracing {
  private GlobalTracing() {}

  /**
   * Return the tracer to be used in a service
   *
   * @return the tracer to be used in a service
   */
  public static Tracer serviceTracer() {
    return GlobalOpenTelemetry.getTracer("service");
  }

  public static void run(String spanName, Runnable runnable) {
    Tracing.run(serviceTracer(), spanName, runnable);
  }

  /**
   * Runs a block of code with a new span - ending the span at the end and recording any exception.
   *
   * @param spanName name of the new span
   */
  public static <T> T call(String spanName, Callable<T> callable) {
    return Tracing.call(serviceTracer(), spanName, callable);
  }

  /**
   * Creates a new span builder with the given span name.
   *
   * @param spanName the span name
   * @return the span builder
   */
  public static SpanBuilder withSpan(String spanName) {
    return serviceTracer().spanBuilder(spanName);
  }

  /**
   * Injects the current context into a string map, which can then be added to HTTP headers or the
   * metadata of an event.
   */
  public static Map<String, String> getPropagationHeaders() {
    return Tracing.getPropagationHeaders(GlobalOpenTelemetry.get());
  }

  /**
   * Extract the context from a string map, which you get from HTTP headers of the metadata of an
   * event you're processing.
   */
  public static Context extractContext(Map<String, String> transport) {
    return Tracing.extractContext(GlobalOpenTelemetry.get(), transport);
  }

  /**
   * Trace a block of code using a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public static <T> T traceServerSpan(
      Map<String, String> transport, SpanBuilder spanBuilder, Callable<T> callable) {
    return Tracing.traceServerSpan(GlobalOpenTelemetry.get(), transport, spanBuilder, callable);
  }

  /**
   * Trace a block of code using a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public static <T> T traceServerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      Callable<T> callable,
      BiConsumer<Span, Exception> handleException) {
    return Tracing.traceServerSpan(
        GlobalOpenTelemetry.get(), transport, spanBuilder, callable, handleException);
  }

  /**
   * Trace a block of code using a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public static <T> T traceConsumerSpan(
      Map<String, String> transport, SpanBuilder spanBuilder, Callable<T> callable) {
    return Tracing.traceConsumerSpan(GlobalOpenTelemetry.get(), transport, spanBuilder, callable);
  }

  /**
   * Trace a block of code using a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public static <T> T traceConsumerSpan(
      Map<String, String> transport,
      SpanBuilder spanBuilder,
      Callable<T> callable,
      BiConsumer<Span, Exception> handleException) {
    return Tracing.traceConsumerSpan(
        GlobalOpenTelemetry.get(), transport, spanBuilder, callable, handleException);
  }
}
