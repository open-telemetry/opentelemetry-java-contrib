package io.opentelemetry.contrib.tracer;/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Map;
import java.util.concurrent.Callable;

/** Provides easy mechanisms for wrapping standard Java constructs with an OpenTelemetry Span. */
public final class ExtendedTracer implements Tracer {

  private final Tracer delegate;

  /** Create a new {@link ExtendedTracer} that wraps the provided Tracer. */
  public static ExtendedTracer create(Tracer delegate) {
    return new ExtendedTracer(delegate);
  }

  private ExtendedTracer(Tracer delegate) {
    this.delegate = delegate;
  }

  /** Run the provided {@link Runnable} and wrap with a {@link Span} with the provided name. */
  public void run(String spanName, Runnable runnable) {
    spanBuilder(spanName).run(runnable);
  }

  /** Call the provided {@link Callable} and wrap with a {@link Span} with the provided name. */
  public <T> T call(String spanName, Callable<T> callable) {
    return spanBuilder(spanName).call(callable);
  }

  /**
   * Injects the current context into a string map, which can then be added to HTTP headers or the
   * metadata of an event.
   */
  public Map<String, String> injectContext() {
    return Tracing.injectContext();
  }

  /**
   * Extract the context from a string map, which you get from HTTP headers of the metadata of an
   * event you're processing.
   */
  public static Context extractContext(Map<String, String> transport) {
    return Tracing.extractContext(transport);
  }

  /** Sets baggage items which are active in given block. */
  public static <T> T setBaggage(Map<String, String> baggage, Callable<T> callable) {
    return Tracing.setBaggage(baggage, callable);
  }

  @Override
  public ExtendedSpanBuilder spanBuilder(String spanName) {
    return new ExtendedSpanBuilder(delegate.spanBuilder(spanName));
  }
}
