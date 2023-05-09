/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * AttributePropagatingSpanProcessor handles the propagation of attributes from parent spans to
 * child spans, specified in {@link #attributesKeysToPropagate}. AttributePropagatingSpanProcessor
 * also propagates the parent span name to child spans, as a new attribute specified by {@link
 * #spanNamePropagationKey}. Span name propagation only starts from local root server/consumer
 * spans, but from there will be propagated to any descendant spans.
 */
@Immutable
public final class AttributePropagatingSpanProcessor implements SpanProcessor {

  private final AttributeKey<String> spanNamePropagationKey;
  private final List<AttributeKey<String>> attributesKeysToPropagate;

  public static AttributePropagatingSpanProcessor create(
      AttributeKey<String> spanNamePropagationKey,
      List<AttributeKey<String>> attributesKeysToPropagate) {
    return new AttributePropagatingSpanProcessor(spanNamePropagationKey, attributesKeysToPropagate);
  }

  private AttributePropagatingSpanProcessor(
      AttributeKey<String> spanNamePropagationKey,
      List<AttributeKey<String>> attributesKeysToPropagate) {
    this.spanNamePropagationKey = spanNamePropagationKey;
    this.attributesKeysToPropagate = attributesKeysToPropagate;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    String spanNameToPropagate;
    if (isLocalRoot(parentReadableSpan.getParentSpanContext())
        && isServerOrConsumer(parentReadableSpan)) {
      spanNameToPropagate = parentReadableSpan.getName();
    } else {
      spanNameToPropagate = parentReadableSpan.getAttribute(spanNamePropagationKey);
    }

    if (spanNameToPropagate != null) {
      span.setAttribute(spanNamePropagationKey, spanNameToPropagate);
    }

    for (AttributeKey<String> keyToPropagate : attributesKeysToPropagate) {
      String valueToPropagate = parentReadableSpan.getAttribute(keyToPropagate);
      if (valueToPropagate != null) {
        span.setAttribute(keyToPropagate, valueToPropagate);
      }
    }
  }

  private static boolean isLocalRoot(SpanContext parentSpanContext) {
    return !parentSpanContext.isValid() || parentSpanContext.isRemote();
  }

  private static boolean isServerOrConsumer(ReadableSpan span) {
    return span.getKind() == SpanKind.SERVER || span.getKind() == SpanKind.CONSUMER;
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
