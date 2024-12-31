/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMinThreshold;
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
public class ConsistentParentBasedSampler extends ConsistentSampler {

  private final Composable rootSampler;

  private final String description;

  /**
   * Constructs a new consistent parent based sampler using the given root sampler and the given
   * thread-safe random generator.
   *
   * @param rootSampler the root sampler
   */
  protected ConsistentParentBasedSampler(Composable rootSampler) {
    this.rootSampler = requireNonNull(rootSampler);
    this.description =
        "ConsistentParentBasedSampler{rootSampler=" + rootSampler.getDescription() + '}';
  }

  @Override
  public final SamplingIntent getSamplingIntent(
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
    boolean isParentAdjustedCountCorrect;
    if (otelTraceState.hasValidThreshold()) {
      parentThreshold = otelTraceState.getThreshold();
      isParentAdjustedCountCorrect = true;
    } else {
      // If no threshold, look at the sampled flag
      parentThreshold = parentSpanContext.isSampled() ? getMinThreshold() : getInvalidThreshold();
      isParentAdjustedCountCorrect = false;
    }

    return new SamplingIntent() {
      @Override
      public long getThreshold() {
        return parentThreshold;
      }

      @Override
      public boolean isAdjustedCountReliable() {
        return isParentAdjustedCountCorrect;
      }

      @Override
      public Attributes getAttributes() {
        if (parentSpanContext.isRemote()) {
          return getAttributesWhenParentRemote(name, spanKind, attributes, parentLinks);
        } else {
          return getAttributesWhenParentLocal(name, spanKind, attributes, parentLinks);
        }
      }

      @Override
      public TraceState updateTraceState(TraceState parentState) {
        return parentState;
      }
    };
  }

  protected Attributes getAttributesWhenParentLocal(
      String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
    return Attributes.empty();
  }

  protected Attributes getAttributesWhenParentRemote(
      String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
    return Attributes.empty();
  }

  @Override
  public String getDescription() {
    return description;
  }
}
