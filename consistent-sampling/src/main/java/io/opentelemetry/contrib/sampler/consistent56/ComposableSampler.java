/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.isValidThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/** Abstract base class for composable consistent samplers. */
public abstract class ComposableSampler extends ConsistentSampler {

  @Override
  public final SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();

    TraceState parentTraceState = parentSpanContext.getTraceState();
    String otelTraceStateString = parentTraceState.get(OtelTraceState.TRACE_STATE_KEY);
    OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);

    SamplingIntent intent =
        getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
    long threshold = intent.getThreshold();

    // determine sampling decision
    boolean isSampled;
    if (isValidThreshold(threshold)) {
      long randomness = getRandomness(otelTraceState, traceId);
      isSampled = threshold <= randomness;
    } else { // DROP
      isSampled = false;
    }

    SamplingDecision samplingDecision;
    if (isSampled) {
      samplingDecision = SamplingDecision.RECORD_AND_SAMPLE;
      otelTraceState.setThreshold(threshold);
    } else {
      samplingDecision = SamplingDecision.DROP;
      otelTraceState.invalidateThreshold();
    }

    String newOtTraceState = otelTraceState.serialize();

    return new SamplingResult() {

      @Override
      public SamplingDecision getDecision() {
        return samplingDecision;
      }

      @Override
      public Attributes getAttributes() {
        return intent.getAttributes();
      }

      @Override
      public TraceState getUpdatedTraceState(TraceState parentTraceState) {
        return intent.updateTraceState(parentTraceState).toBuilder()
            .put(OtelTraceState.TRACE_STATE_KEY, newOtTraceState)
            .build();
      }
    };
  }

  private static long getRandomness(OtelTraceState otelTraceState, String traceId) {
    if (otelTraceState.hasValidRandomValue()) {
      return otelTraceState.getRandomValue();
    } else {
      return OtelTraceState.parseHex(traceId, 18, 14, getInvalidRandomValue());
    }
  }

  @Override
  protected final long getThreshold(long parentThreshold, boolean isRoot) {
    // not used by Composable Samplers
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the SamplingIntent that is used for the sampling decision. The SamplingIntent includes
   * the threshold value which will be used for the sampling decision.
   *
   * <p>NOTE: Keep in mind, that in any case the returned threshold value must not depend directly
   * or indirectly on the random value. In particular this means that the parent sampled flag must
   * not be used for the calculation of the threshold as the sampled flag depends itself on the
   * random value.
   */
  protected abstract SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks);
}
