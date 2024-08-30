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
  static Predicate forRootSpan() {
    return (parentContext, name, spanKind, attributes, parentLinks) -> {
      Span parentSpan = Span.fromContext(parentContext);
      SpanContext parentSpanContext = parentSpan.getSpanContext();
      return !parentSpanContext.isValid();
    };
  }

  /*
   * Return a Predicate that matches all Spans
   */
  static Predicate forAnySpan() {
    return (parentContext, name, spanKind, attributes, parentLinks) -> true;
  }

  // TO DO: allow for composing Predicates: and(p1,p2), or(p1,p2), and not(p1).
}
