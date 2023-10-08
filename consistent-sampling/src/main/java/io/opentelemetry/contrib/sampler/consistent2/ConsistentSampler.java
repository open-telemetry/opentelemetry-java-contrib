/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getMaxThreshold;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.isValidThreshold;
import static java.util.Objects.requireNonNull;

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

/** Abstract base class for consistent samplers. */
public abstract class ConsistentSampler implements Sampler {

  /**
   * Returns a {@link ConsistentSampler} that samples all spans.
   *
   * @return a sampler
   */
  public static ConsistentSampler alwaysOn() {
    return alwaysOn(RandomValueGenerators.getDefault());
  }

  /**
   * Returns a {@link ConsistentSampler} that samples all spans.
   *
   * @param randomValueGenerator the function to use for generating the random value
   * @return a sampler
   */
  public static ConsistentSampler alwaysOn(RandomValueGenerator randomValueGenerator) {
    return new ConsistentAlwaysOnSampler(randomValueGenerator);
  }

  /**
   * Returns a {@link ConsistentSampler} that does not sample any span.
   *
   * @return a sampler
   */
  public static ConsistentSampler alwaysOff() {
    return alwaysOff(RandomValueGenerators.getDefault());
  }

  /**
   * Returns a {@link ConsistentSampler} that does not sample any span.
   *
   * @param randomValueGenerator the function to use for generating the random value
   * @return a sampler
   */
  public static ConsistentSampler alwaysOff(RandomValueGenerator randomValueGenerator) {
    return new ConsistentAlwaysOffSampler(randomValueGenerator);
  }

  /**
   * Returns a {@link ConsistentSampler} that samples each span with a fixed probability.
   *
   * @param samplingProbability the sampling probability
   * @return a sampler
   */
  public static ConsistentSampler probabilityBased(double samplingProbability) {
    return probabilityBased(samplingProbability, RandomValueGenerators.getDefault());
  }

  /**
   * Returns a {@link ConsistentSampler} that samples each span with a fixed probability.
   *
   * @param samplingProbability the sampling probability
   * @param randomValueGenerator the function to use for generating the r-value
   * @return a sampler
   */
  public static ConsistentSampler probabilityBased(
      double samplingProbability, RandomValueGenerator randomValueGenerator) {
    long threshold = ConsistentSamplingUtil.calculateThreshold(samplingProbability);
    return new ConsistentFixedThresholdSampler(threshold, randomValueGenerator);
  }

  /**
   * Returns a new {@link ConsistentSampler} that respects the sampling decision of the parent span
   * or falls-back to the given sampler if it is a root span.
   *
   * @param rootSampler the root sampler
   */
  public static ConsistentSampler parentBased(ConsistentSampler rootSampler) {
    return parentBased(rootSampler, RandomValueGenerators.getDefault());
  }

  /**
   * Returns a new {@link ConsistentSampler} that respects the sampling decision of the parent span
   * or falls-back to the given sampler if it is a root span.
   *
   * @param rootSampler the root sampler
   * @param randomValueGenerator the function to use for generating the random value
   */
  public static ConsistentSampler parentBased(
      ConsistentSampler rootSampler, RandomValueGenerator randomValueGenerator) {
    return new ConsistentParentBasedSampler(rootSampler, randomValueGenerator);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   */
  public static ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return rateLimited(
        targetSpansPerSecondLimit, adaptationTimeSeconds, RandomValueGenerators.getDefault());
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param randomValueGenerator the function to use for generating the random value
   */
  public static ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      RandomValueGenerator randomValueGenerator) {
    return rateLimited(
        targetSpansPerSecondLimit, adaptationTimeSeconds, randomValueGenerator, System::nanoTime);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param randomValueGenerator the function to use for generating the random value
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  static ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      RandomValueGenerator randomValueGenerator,
      LongSupplier nanoTimeSupplier) {
    return new ConsistentRateLimitingSampler(
        targetSpansPerSecondLimit, adaptationTimeSeconds, randomValueGenerator, nanoTimeSupplier);
  }

  /**
   * Returns a {@link ConsistentSampler} that samples a span if both this and the other given
   * consistent sampler would sample the span.
   *
   * <p>If the other consistent sampler is the same as this, this consistent sampler will be
   * returned.
   *
   * <p>The returned sampler takes care of setting the trace state correctly, which would not happen
   * if the {@link #shouldSample(Context, String, String, SpanKind, Attributes, List)} method was
   * called for each sampler individually. Also, the combined sampler is more efficient than
   * evaluating the two samplers individually and combining both results afterwards.
   *
   * @param otherConsistentSampler the other consistent sampler
   * @return the composed consistent sampler
   */
  public ConsistentSampler and(ConsistentSampler otherConsistentSampler) {
    if (otherConsistentSampler == this) {
      return this;
    }
    return new ConsistentComposedAndSampler(
        this, otherConsistentSampler, RandomValueGenerators.getDefault());
  }

  /**
   * Returns a {@link ConsistentSampler} that samples a span if this or the other given consistent
   * sampler would sample the span.
   *
   * <p>If the other consistent sampler is the same as this, this consistent sampler will be
   * returned.
   *
   * <p>The returned sampler takes care of setting the trace state correctly, which would not happen
   * if the {@link #shouldSample(Context, String, String, SpanKind, Attributes, List)} method was
   * called for each sampler individually. Also, the combined sampler is more efficient than
   * evaluating the two samplers individually and combining both results afterwards.
   *
   * @param otherConsistentSampler the other consistent sampler
   * @return the composed consistent sampler
   */
  public ConsistentSampler or(ConsistentSampler otherConsistentSampler) {
    if (otherConsistentSampler == this) {
      return this;
    }
    return new ConsistentComposedOrSampler(
        this, otherConsistentSampler, RandomValueGenerators.getDefault());
  }

  private final RandomValueGenerator randomValueGenerator;

  protected ConsistentSampler(RandomValueGenerator randomValueGenerator) {
    this.randomValueGenerator = requireNonNull(randomValueGenerator);
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
    boolean isRoot = !parentSpanContext.isValid();
    boolean isParentSampled = parentSpanContext.isSampled();

    boolean isRandomTraceIdFlagSet = false; // TODO in future get the random trace ID flag, compare
    // https://www.w3.org/TR/trace-context-2/#random-trace-id-flag

    TraceState parentTraceState = parentSpanContext.getTraceState();
    String otelTraceStateString = parentTraceState.get(OtelTraceState.TRACE_STATE_KEY);
    OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);

    long randomValue;
    if (otelTraceState.hasValidRandomValue()) {
      randomValue = otelTraceState.getRandomValue();
    } else if (isRandomTraceIdFlagSet) {
      randomValue = OtelTraceState.parseHex(traceId, 18, 14, getInvalidRandomValue());
    } else {
      randomValue = randomValueGenerator.generate(traceId);
      otelTraceState.invalidateThreshold();
      otelTraceState.setRandomValue(randomValue);
    }

    long parentThreshold;
    if (otelTraceState.hasValidThreshold()) {
      long threshold = otelTraceState.getThreshold();
      if (((randomValue < threshold) == isParentSampled) || threshold == 0) {
        parentThreshold = threshold;
      } else {
        parentThreshold = getInvalidThreshold();
      }
    } else if (isParentSampled) {
      parentThreshold = getMaxThreshold();
    } else {
      parentThreshold = 0;
    }

    // determine new threshold that is used for the sampling decision
    long threshold = getThreshold(parentThreshold, isRoot);

    // determine sampling decision
    boolean isSampled;
    if (isValidThreshold(threshold)) {
      isSampled = (randomValue < threshold);
      if (0 < threshold && threshold < getMaxThreshold()) {
        otelTraceState.setThreshold(threshold);
      } else {
        otelTraceState.invalidateThreshold();
      }
    } else {
      isSampled = isParentSampled;
      otelTraceState.invalidateThreshold();
    }

    SamplingDecision samplingDecision =
        isSampled ? SamplingDecision.RECORD_AND_SAMPLE : SamplingDecision.DROP;

    String newOtTraceState = otelTraceState.serialize();

    return new SamplingResult() {

      @Override
      public SamplingDecision getDecision() {
        return samplingDecision;
      }

      @Override
      public Attributes getAttributes() {
        return Attributes.empty();
      }

      @Override
      public TraceState getUpdatedTraceState(TraceState parentTraceState) {
        return parentTraceState.toBuilder()
            .put(OtelTraceState.TRACE_STATE_KEY, newOtTraceState)
            .build();
      }
    };
  }

  /**
   * Returns the threshold that is used for the sampling decision.
   *
   * <p>NOTE: In future, further information like span attributes could be also added as arguments
   * such that the sampling probability could be made dependent on those extra arguments. However,
   * in any case the returned threshold value must not depend directly or indirectly on the random
   * value. In particular this means that the parent sampled flag must not be used for the
   * calculation of the threshold as the sampled flag depends itself on the random value.
   *
   * @param parentThreshold is the threshold (if known) that was used for a consistent sampling
   *     decision by the parent
   * @param isRoot is true for the root span
   * @return the threshold to be used for the sampling decision
   */
  protected abstract long getThreshold(long parentThreshold, boolean isRoot);
}
