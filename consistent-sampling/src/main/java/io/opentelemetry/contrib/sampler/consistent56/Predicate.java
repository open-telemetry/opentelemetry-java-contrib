/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;

/** Interface for logical expression that can be matched against Spans to be sampled */
@FunctionalInterface
public interface Predicate {

  /*
   * Return true if the Span context described by the provided arguments matches the predicate
   */
  boolean spanMatches(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks);

  /*
   * Return a Predicate that will match ROOT Spans only
   */
  static Predicate isRootSpan() {
    return (parentContext, name, spanKind, attributes, parentLinks) -> {
      Span parentSpan = Span.fromContext(parentContext);
      SpanContext parentSpanContext = parentSpan.getSpanContext();
      return !parentSpanContext.isValid();
    };
  }

  /*
   * Return a Predicate that matches all Spans
   */
  static Predicate anySpan() {
    return (parentContext, name, spanKind, attributes, parentLinks) -> true;
  }

  /*
   * Return a Predicate that represents logical AND of the argument predicates
   */
  static Predicate and(Predicate p1, Predicate p2) {
    return (parentContext, name, spanKind, attributes, parentLinks) ->
        p1.spanMatches(parentContext, name, spanKind, attributes, parentLinks)
            && p2.spanMatches(parentContext, name, spanKind, attributes, parentLinks);
  }

  /*
   * Return a Predicate that represents logical negation of the argument predicate
   */
  static Predicate not(Predicate p) {
    return (parentContext, name, spanKind, attributes, parentLinks) ->
        !p.spanMatches(parentContext, name, spanKind, attributes, parentLinks);
  }

  /*
   * Return a Predicate that represents logical OR of the argument predicates
   */
  static Predicate or(Predicate p1, Predicate p2) {
    return (parentContext, name, spanKind, attributes, parentLinks) ->
        p1.spanMatches(parentContext, name, spanKind, attributes, parentLinks)
            || p2.spanMatches(parentContext, name, spanKind, attributes, parentLinks);
  }
}
