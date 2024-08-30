/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that makes the same sampling decision as the parent. For root spans the
 * sampling decision is delegated to the root sampler.
 */
@Immutable
final class ConsistentParentBasedSampler extends ConsistentSampler {

  private final ComposableSampler rootSampler;

  private final String description;

  /**
   * Constructs a new consistent parent based sampler using the given root sampler and the given
   * thread-safe random generator.
   *
   * @param rootSampler the root sampler
   */
  ConsistentParentBasedSampler(ComposableSampler rootSampler) {
    this.rootSampler = requireNonNull(rootSampler);
    this.description =
        "ConsistentParentBasedSampler{rootSampler=" + rootSampler.getDescription() + '}';
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    boolean isRoot = !parentSpanContext.isValid();

    if (isRoot) {
      return rootSampler.getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
    }

    TraceState parentTraceState = parentSpanContext.getTraceState();
    String otelTraceStateString = parentTraceState.get(OtelTraceState.TRACE_STATE_KEY);
    OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);

    long parentThreshold;
    if (otelTraceState.hasValidThreshold()) {
      parentThreshold = otelTraceState.getThreshold();
    } else {
      parentThreshold = getInvalidThreshold();
    }

    return () -> parentThreshold;
  }

  @Override
  public String getDescription() {
    return description;
  }
}
