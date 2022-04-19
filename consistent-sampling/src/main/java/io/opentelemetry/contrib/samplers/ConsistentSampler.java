/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.state.OtelTraceState;
import io.opentelemetry.contrib.util.DefaultRandomGenerator;
import io.opentelemetry.contrib.util.RandomGenerator;
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
  public static final ConsistentSampler alwaysOn() {
    return ConsistentAlwaysOnSampler.getInstance();
  }

  /**
   * Returns a {@link ConsistentSampler} that does not sample any span.
   *
   * @return a sampler
   */
  public static final ConsistentSampler alwaysOff() {
    return ConsistentAlwaysOffSampler.getInstance();
  }

  /**
   * Returns a {@link ConsistentSampler} that samples each span with a fixed probability.
   *
   * @param samplingProbability the sampling probability
   * @return a sampler
   */
  public static final ConsistentSampler probabilityBased(double samplingProbability) {
    return new ConsistentProbabilityBasedSampler(samplingProbability);
  }

  /**
   * Returns a {@link ConsistentSampler} that samples each span with a fixed probability.
   *
   * @param samplingProbability the sampling probability
   * @param threadSafeRandomGenerator a thread-safe random generator
   * @return a sampler
   */
  public static final ConsistentSampler probabilityBased(
      double samplingProbability, RandomGenerator threadSafeRandomGenerator) {
    return new ConsistentProbabilityBasedSampler(samplingProbability, threadSafeRandomGenerator);
  }

  /**
   * Returns a new {@link ConsistentSampler} that respects the sampling decision of the parent span
   * or falls-back to the given sampler if it is a root span.
   *
   * @param rootSampler the root sampler
   */
  public static final ConsistentSampler parentBased(ConsistentSampler rootSampler) {
    return new ConsistentParentBasedSampler(rootSampler);
  }

  /**
   * Returns a new {@link ConsistentSampler} that respects the sampling decision of the parent span
   * or falls-back to the given sampler if it is a root span.
   *
   * @param rootSampler the root sampler
   * @param threadSafeRandomGenerator a thread-safe random generator
   */
  public static final ConsistentSampler parentBased(
      ConsistentSampler rootSampler, RandomGenerator threadSafeRandomGenerator) {
    return new ConsistentParentBasedSampler(rootSampler, threadSafeRandomGenerator);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   */
  public static final ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return new ConsistentRateLimitingSampler(targetSpansPerSecondLimit, adaptationTimeSeconds);
  }

  /**
   * Returns a new {@link ConsistentSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param threadSafeRandomGenerator a thread-safe random generator
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  public static final ConsistentSampler rateLimited(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      RandomGenerator threadSafeRandomGenerator,
      LongSupplier nanoTimeSupplier) {
    return new ConsistentRateLimitingSampler(
        targetSpansPerSecondLimit,
        adaptationTimeSeconds,
        threadSafeRandomGenerator,
        nanoTimeSupplier);
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
    return new ConsistentComposedAndSampler(this, otherConsistentSampler);
  }

  /**
   * Returns a {@link ConsistentSampler} that samples a span if either this or the other given
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
  public ConsistentSampler or(ConsistentSampler otherConsistentSampler) {
    if (otherConsistentSampler == this) {
      return this;
    }
    return new ConsistentComposedOrSampler(this, otherConsistentSampler);
  }

  protected final RandomGenerator threadSafeRandomGenerator;

  protected ConsistentSampler(RandomGenerator threadSafeRandomGenerator) {
    this.threadSafeRandomGenerator = requireNonNull(threadSafeRandomGenerator);
  }

  protected ConsistentSampler() {
    this(DefaultRandomGenerator.get());
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

    TraceState parentTraceState = parentSpanContext.getTraceState();
    String otelTraceStateString = parentTraceState.get(OtelTraceState.TRACE_STATE_KEY);
    OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);

    if (!otelTraceState.hasValidR()) {
      otelTraceState.invalidateP();
    }
    // Invariant checking: unset p-value when p-value, r-value, and isParentSampled are inconsistent
    if (otelTraceState.hasValidR() && otelTraceState.hasValidP()) {
      if ((((otelTraceState.getP() <= otelTraceState.getR()) == isParentSampled)
              || (isParentSampled && (otelTraceState.getP() == OtelTraceState.getMaxP())))
          == false) {
        otelTraceState.invalidateP();
      }
    }

    // generate new r-value if not available
    if (!otelTraceState.hasValidR()) {
      otelTraceState.setR(
          Math.min(
              threadSafeRandomGenerator.numberOfLeadingZerosOfRandomLong(),
              OtelTraceState.getMaxR()));
    }

    // determine and set new p-value that is used for the sampling decision
    int newP = getP(otelTraceState.getP(), isRoot);
    otelTraceState.setP(newP);

    // determine sampling decision
    boolean isSampled;
    if (otelTraceState.hasValidP()) {
      isSampled = (otelTraceState.getP() <= otelTraceState.getR());
    } else {
      // if new p-value is invalid, respect sampling decision of parent
      isSampled = isParentSampled;
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
   * Returns the p-value that is used for the sampling decision.
   *
   * <p>The returned p-value is translated into corresponding sampling probabilities as given in the
   * following:
   *
   * <p>p-value = 0 => sampling probability = 1
   *
   * <p>p-value = 1 => sampling probability = 1/2
   *
   * <p>p-value = 2 => sampling probability = 1/4
   *
   * <p>...
   *
   * <p>p-value = (z-2) => sampling probability = 1/2^(z-2)
   *
   * <p>p-value = (z-1) => sampling probability = 1/2^(z-1)
   *
   * <p>p-value = z => sampling probability = 0
   *
   * <p>Here z denotes OtelTraceState.getMaxP().
   *
   * <p>Any other p-values have no meaning and will lead to inconsistent sampling decisions. The
   * parent sampled flag will define the sampling decision in this case.
   *
   * <p>NOTE: In future, further information like span attributes could be also added as arguments
   * such that the sampling probability could be made dependent on those extra arguments. However,
   * in any case the returned p-value must not depend directly or indirectly on the r-value. In
   * particular this means that the parent sampled flag must not be used for the calculation of the
   * p-value as the sampled flag depends itself on the r-value.
   *
   * @param parentP is the p-value (if known) that was used for a consistent sampling decision by
   *     the parent
   * @param isRoot is true for the root span
   * @return this Builder
   */
  protected abstract int getP(int parentP, boolean isRoot);

  /**
   * Returns the sampling probability for a given p-value.
   *
   * @param p the p-value
   * @return the sampling probability in the range [0,1] or Double.Nan if the p-value is invalid
   */
  protected static double getSamplingProbability(int p) {
    if (OtelTraceState.isValidP(p)) {
      if (p == OtelTraceState.getMaxP()) {
        return 0.;
      } else {
        return Double.longBitsToDouble((0x3FFL - p) << 52);
      }
    } else {
      return Double.NaN;
    }
  }

  private static final double SMALLEST_POSITIVE_SAMPLING_PROBABILITY =
      getSamplingProbability(OtelTraceState.getMaxP() - 1);

  /**
   * Returns the largest p-value for which {@code getSamplingProbability(p) >= samplingProbability}.
   *
   * @param samplingProbability the sampling probability
   * @return the p-value
   */
  protected static int getLowerBoundP(double samplingProbability) {
    if (!(samplingProbability >= 0. && samplingProbability <= 1.)) {
      throw new IllegalArgumentException();
    }
    if (samplingProbability <= SMALLEST_POSITIVE_SAMPLING_PROBABILITY) {
      return OtelTraceState.getMaxP() - (samplingProbability > 0. ? 1 : 0);
    } else {
      long longSamplingProbability = Double.doubleToRawLongBits(samplingProbability);
      long mantissa = longSamplingProbability & 0x000FFFFFFFFFFFFFL;
      long exponent = longSamplingProbability >>> 52;
      return (int) (0x3FFL - exponent) - (mantissa != 0 ? 1 : 0);
    }
  }

  /**
   * Returns the smallest p-value for which {@code getSamplingProbability(p) <=
   * samplingProbability}.
   *
   * @param samplingProbability the sampling probability
   * @return the p-value
   */
  protected static int getUpperBoundP(double samplingProbability) {
    if (!(samplingProbability >= 0. && samplingProbability <= 1.)) {
      throw new IllegalArgumentException();
    }
    if (samplingProbability <= SMALLEST_POSITIVE_SAMPLING_PROBABILITY) {
      return OtelTraceState.getMaxP();
    } else {
      long longSamplingProbability = Double.doubleToRawLongBits(samplingProbability);
      long exponent = longSamplingProbability >>> 52;
      return (int) (0x3FFL - exponent);
    }
  }
}
