package io.opentelemetry.contrib.tracer;/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ExtendedSpanBuilder implements SpanBuilder {

  private final SpanBuilder delegate;

  ExtendedSpanBuilder(SpanBuilder delegate) {
    this.delegate = delegate;
  }

  /** Run the provided {@link Runnable} and wrap with a {@link Span} with the provided name. */
  public void run(Runnable runnable) {
    Tracing.run(startSpan(), runnable);
  }

  /** Call the provided {@link Callable} and wrap with a {@link Span} with the provided name. */
  public <T> T call(Callable<T> callable) {
    return Tracing.call(startSpan(), callable);
  }

  /**
   * Trace a block of code using a server span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceServerSpan(Map<String, String> transport, Callable<T> callable) {
    return Tracing.traceServerSpan(transport, this, callable);
  }

  /**
   * Trace a block of code using a consumer span.
   *
   * <p>The span context will be extracted from the <code>transport</code>, which you usually get
   * from HTTP headers of the metadata of an event you're processing.
   */
  public <T> T traceConsumerSpan(
      Map<String, String> transport, SpanBuilder spanBuilder, Callable<T> callable) {
    return Tracing.traceConsumerSpan(transport, this, callable);
  }

  @Override
  public SpanBuilder setParent(Context context) {
    return delegate.setParent(context);
  }

  @Override
  public SpanBuilder setNoParent() {
    return delegate.setNoParent();
  }

  @Override
  public SpanBuilder addLink(SpanContext spanContext) {
    return delegate.addLink(spanContext);
  }

  @Override
  public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
    return delegate.addLink(spanContext, attributes);
  }

  @Override
  public SpanBuilder setAttribute(String key, String value) {
    return delegate.setAttribute(key, value);
  }

  @Override
  public SpanBuilder setAttribute(String key, long value) {
    return delegate.setAttribute(key, value);
  }

  @Override
  public SpanBuilder setAttribute(String key, double value) {
    return delegate.setAttribute(key, value);
  }

  @Override
  public SpanBuilder setAttribute(String key, boolean value) {
    return delegate.setAttribute(key, value);
  }

  @Override
  public <T> SpanBuilder setAttribute(AttributeKey<T> key, T value) {
    return delegate.setAttribute(key, value);
  }

  @Override
  public SpanBuilder setAllAttributes(Attributes attributes) {
    return delegate.setAllAttributes(attributes);
  }

  @Override
  public SpanBuilder setSpanKind(SpanKind spanKind) {
    return delegate.setSpanKind(spanKind);
  }

  @Override
  public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
    return delegate.setStartTimestamp(startTimestamp, unit);
  }

  @Override
  public SpanBuilder setStartTimestamp(Instant startTimestamp) {
    return delegate.setStartTimestamp(startTimestamp);
  }

  @Override
  public Span startSpan() {
    return delegate.startSpan();
  }
}
