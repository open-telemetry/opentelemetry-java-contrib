/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.List;
import javax.annotation.Nullable;

public class MessagingProcessWrapper<REQUEST> {

  private static final String INSTRUMENTATION_SCOPE = "messaging-process-wrapper";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private final TextMapPropagator textMapPropagator;

  private final Tracer tracer;

  private final TextMapGetter<REQUEST> textMapGetter;

  private final SpanNameExtractor<REQUEST> spanNameExtractor;

  // no attributes need to be extracted from responses in process operations
  private final List<AttributesExtractor<REQUEST, Void>> attributesExtractors;

  public static <REQUEST> MessagingProcessWrapperBuilder<REQUEST> defaultBuilder() {
    return new MessagingProcessWrapperBuilder<>();
  }

  public <E extends Throwable> void doProcess(REQUEST request, ThrowingRunnable<E> runnable)
      throws E {
    Span span = handleStart(request);
    Context context = span.storeInContext(Context.current());

    try (Scope scope = context.makeCurrent()) {
      runnable.run();
    } catch (Throwable t) {
      handleEnd(span, context, request, t);
      throw t;
    }

    handleEnd(span, context, request, null);
  }

  public <R, E extends Throwable> R doProcess(REQUEST request, ThrowingSupplier<R, E> supplier)
      throws E {
    Span span = handleStart(request);
    Context context = span.storeInContext(Context.current());

    R result = null;
    try (Scope scope = context.makeCurrent()) {
      result = supplier.get();
    } catch (Throwable t) {
      handleEnd(span, context, request, t);
      throw t;
    }

    handleEnd(span, context, request, null);
    return result;
  }

  protected Span handleStart(REQUEST request) {
    Context context =
        this.textMapPropagator.extract(Context.current(), request, this.textMapGetter);
    SpanBuilder spanBuilder = this.tracer.spanBuilder(spanNameExtractor.extract(request));
    spanBuilder.setSpanKind(CONSUMER).setParent(context);

    AttributesBuilder builder = Attributes.builder();
    for (AttributesExtractor<REQUEST, Void> extractor : this.attributesExtractors) {
      extractor.onStart(builder, context, request);
    }
    return spanBuilder.setAllAttributes(builder.build()).startSpan();
  }

  protected void handleEnd(Span span, Context context, REQUEST request, @Nullable Throwable t) {
    AttributesBuilder builder = Attributes.builder();
    for (AttributesExtractor<REQUEST, Void> extractor : this.attributesExtractors) {
      extractor.onEnd(builder, context, request, null, t);
    }

    span.setAllAttributes(builder.build());
    span.end();
  }

  protected MessagingProcessWrapper(
      OpenTelemetry openTelemetry,
      TextMapGetter<REQUEST> textMapGetter,
      SpanNameExtractor<REQUEST> spanNameExtractor,
      List<AttributesExtractor<REQUEST, Void>> attributesExtractors) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
    this.textMapGetter = textMapGetter;
    this.spanNameExtractor = spanNameExtractor;
    this.attributesExtractors = attributesExtractors;
  }
}
