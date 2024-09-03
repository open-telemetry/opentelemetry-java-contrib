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
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;

/** Abstract base class for consistent samplers. */
@SuppressWarnings("InconsistentOverloads")
public abstract class ConsistentSampler implements Sampler, ComposableSampler {

  /**
   * Returns a {@link ConsistentSampler} that samples all spans.
   *
   * @return a sampler
   */
  public static ConsistentSampler alwaysOn() {
    return ConsistentAlwaysOnSampler.getInstance();
  }

  /**
   * Returns a {@link ConsistentSampler} that does not sample any span.
   *
   * @return a sampler
   */
  public static ConsistentSampler alwaysOff() {
    return ConsistentAlwaysOffSampler.getInstance();
  }

  /**
   * Returns a {@link ConsistentSampler} that samples each span with a fixed probability.
   *
   * @param samplingProbability the sampling probability
   * @return a sampler
   */
  public static ConsistentSampler probabilityBased(double samplingProbability) {
    long threshold = ConsistentSamplingUtil.calculateThreshold(samplingProbability);
    return new ConsistentFixedThresholdSampler(threshold);
  }

  /**
   * Returns a new {@link ConsistentSampler} that respects the sampling decision of the parent span
   * or falls-back to the given sampler if it is a root span.
   *
   * @param rootSampler the root sampler
   */
  public static ConsistentSampler parentBased(ComposableSampler rootSampler) {
    return new ConsistentParentBasedSampler(rootSampler);
  }

  /**
   * Constructs a new consistent rule based sampler using the given sequence of Predicates and
   * delegate Samplers.
   *
   * @param spanKindToMatch the SpanKind for which the Sampler applies, null value indicates all
   *     SpanKinds
   * @param samplers the PredicatedSamplers to evaluate and query
   */
  public static ConsistentRuleBasedSampler ruleBased(
      @Nullable SpanKind spanKindToMatch, PredicatedSampler... samplers) {
    return new ConsistentRuleBasedSampler(spanKindToMatch, samplers);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   */
  static ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return rateLimited(alwaysOn(), targetSpansPerSecondLimit, adaptationTimeSeconds);
  }

  /**
   * Returns a new {@link ConsistentSampler} that honors the delegate sampling decision as long as
   * it seems to meet the target span rate. In case the delegate sampling rate seems to exceed the
   * target, the sampler attempts to decrease the effective sampling probability dynamically to meet
   * the target span rate.
   *
   * @param delegate the delegate sampler
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   */
  public static ConsistentSampler rateLimited(
      ComposableSampler delegate, double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return rateLimited(
        delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, System::nanoTime);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  static ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier) {
    return rateLimited(
        alwaysOn(), targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);
  }

  /**
   * Returns a new {@link ConsistentSampler} that honors the delegate sampling decision as long as
   * it seems to meet the target span rate. In case the delegate sampling rate seems to exceed the
   * target, the sampler attempts to decrease the effective sampling probability dynamically to meet
   * the target span rate.
   *
   * @param delegate the delegate sampler
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  static ConsistentSampler rateLimited(
      ComposableSampler delegate,
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier) {
    return new ConsistentRateLimitingSampler(
        delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);
  }

  /**
   * Returns a {@link ConsistentSampler} that queries its delegate Samplers for their sampling
   * threshold before determining what threshold to use. The intention is to make a positive
   * sampling decision if any of the delegates would make a positive decision.
   *
   * <p>The returned sampler takes care of setting the trace state correctly, which would not happen
   * if the {@link #shouldSample(Context, String, String, SpanKind, Attributes, List)} method was
   * called for each sampler individually. Also, the combined sampler is more efficient than
   * evaluating the samplers individually and combining the results afterwards.
   *
   * @param delegates the delegate samplers, at least one delegate must be specified
   * @return the ConsistentAnyOf sampler
   */
  public static ConsistentSampler anyOf(ComposableSampler... delegates) {
    return new ConsistentAnyOf(delegates);
  }

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
}
