/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.isValidThreshold;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import javax.annotation.concurrent.Immutable;

/**
 * This consistent {@link Sampler} adjusts the sampling probability dynamically to limit the rate of
 * sampled spans.
 *
 * <p>This sampler uses exponential smoothing to estimate on irregular data (compare Wright, David
 * J. "Forecasting data published at irregular time intervals using an extension of Holt's method."
 * Management science 32.4 (1986): 499-510.) to estimate the average waiting time between spans
 * which further allows to estimate the current rate of spans. In the paper, Eq. 2 defines the
 * weighted average of a sequence of data
 *
 * <p>{@code ..., X(n-2), X(n-1), X(n)}
 *
 * <p>at irregular times
 *
 * <p>{@code ..., t(n-2), t(n-1), t(n)}
 *
 * <p>as
 *
 * <p>{@code E(X(n)) := A(n) * V(n)}.
 *
 * <p>{@code A(n)} and {@code V(n)} are computed recursively using Eq. 5 and Eq. 6 given by
 *
 * <p>{@code A(n) = b(n) * A(n-1) + X(n)} and {@code V(n) = V(n-1) / (b(n) + V(n-1))}
 *
 * <p>where
 *
 * <p>{@code b(n) := (1 - a)^(t(n) - t(n-1)) = exp((t(n) - t(n-1)) * ln(1 - a))}.
 *
 * <p>Introducing
 *
 * <p>{@code C(n) := 1 / V(n)}
 *
 * <p>the recursion can be rewritten as
 *
 * <p>{@code A(n) = b(n) * A(n-1) + X(n)} and {@code C(n) = b(n) * C(n-1) + 1}.
 *
 * <p>
 *
 * <p>Since we want to estimate the average waiting time, our data is given by
 *
 * <p>{@code X(n) := t(n) - t(n-1)}.
 *
 * <p>
 *
 * <p>The following correspondence is used for the implementation:
 *
 * <ul>
 *   <li>{@code effectiveWindowNanos} corresponds to {@code A(n)}
 *   <li>{@code effectiveWindowCount} corresponds to {@code C(n)}
 *   <li>{@code decayFactor} corresponds to {@code b(n)}
 *   <li>{@code adaptationTimeSeconds} corresponds to {@code -1 / ln(1 - a)}
 * </ul>
 *
 * <p>
 *
 * <p>The sampler also keeps track of the average sampling probability delivered by the delegate
 * sampler, using exponential smoothing. Given the sequence of the observed probabilities {@code
 * P(k)}, the exponentially smoothed values {@code S(k)} are calculated according to the following
 * formula:
 *
 * <p>{@code S(0) = 1}
 *
 * <p>{@code S(n) = alpha * P(n) + (1 - alpha) * S(n-1)}, for {@code n > 0}
 *
 * <p>where {@code alpha} is the smoothing factor ({@code 0 < alpha < 1}).
 *
 * <p>The smoothing factor is chosen heuristically to be approximately proportional to the expected
 * maximum volume of spans sampled within the adaptation time window, i.e.
 *
 * <p>{@code 1 / (adaptationTimeSeconds * targetSpansPerSecondLimit)}
 */
final class ConsistentRateLimitingSampler extends ConsistentSampler {

  private static final double NANOS_IN_SECONDS = 1e-9;

  @Immutable
  private static final class State {
    private final double effectiveWindowCount;
    private final double effectiveWindowNanos;
    private final double effectiveDelegateProbability;
    private final long lastNanoTime;

    public State(
        double effectiveWindowCount,
        double effectiveWindowNanos,
        long lastNanoTime,
        double effectiveDelegateProbability) {
      this.effectiveWindowCount = effectiveWindowCount;
      this.effectiveWindowNanos = effectiveWindowNanos;
      this.lastNanoTime = lastNanoTime;
      this.effectiveDelegateProbability = effectiveDelegateProbability;
    }
  }

  private final String description;
  private final LongSupplier nanoTimeSupplier;
  private final double inverseAdaptationTimeNanos;
  private final double targetSpansPerNanosecondLimit;
  private final double probabilitySmoothingFactor;
  private final AtomicReference<State> state;
  private final ComposableSampler delegate;

  /**
   * Constructor.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  ConsistentRateLimitingSampler(
      ComposableSampler delegate,
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier) {

    this.delegate = requireNonNull(delegate);

    if (targetSpansPerSecondLimit < 0.0) {
      throw new IllegalArgumentException("Limit for sampled spans per second must be nonnegative!");
    }
    if (adaptationTimeSeconds < 0.0) {
      throw new IllegalArgumentException("Adaptation rate must be nonnegative!");
    }
    this.description =
        "ConsistentRateLimitingSampler{targetSpansPerSecondLimit="
            + targetSpansPerSecondLimit
            + ", adaptationTimeSeconds="
            + adaptationTimeSeconds
            + "}";
    this.nanoTimeSupplier = requireNonNull(nanoTimeSupplier);

    this.inverseAdaptationTimeNanos = NANOS_IN_SECONDS / adaptationTimeSeconds;
    this.targetSpansPerNanosecondLimit = NANOS_IN_SECONDS * targetSpansPerSecondLimit;

    this.probabilitySmoothingFactor =
        determineProbabilitySmoothingFactor(targetSpansPerSecondLimit, adaptationTimeSeconds);

    this.state = new AtomicReference<>(new State(0, 0, nanoTimeSupplier.getAsLong(), 1.0));
  }

  private static double determineProbabilitySmoothingFactor(
      double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    // The probability smoothing factor alpha will be the weight for the newly observed
    // probability P, while (1-alpha) will be the weight for the cumulative average probability
    // observed so far (newC = P * alpha + oldC * (1 - alpha)). Any smoothing factor
    // alpha from the interval (0.0, 1.0) is mathematically acceptable.
    // However, we'd like the weight associated with the newly observed data point to be inversely
    // proportional to the adaptation time (larger adaptation time will allow longer time for the
    // cumulative probability to stabilize) and inversely proportional to the order of magnitude of
    // the data points arriving within a given time unit (because with a lot of data points we can
    // afford to give a smaller weight to each single one). We do not know the true rate of Spans
    // coming in to get sampled, but we optimistically assume that the user knows what they are
    // doing and that the targetSpansPerSecondLimit will be of similar order of magnitude.

    // First approximation of the probability smoothing factor alpha.
    double t = 1.0 / (targetSpansPerSecondLimit * adaptationTimeSeconds);

    // We expect that t is a small number, but we have to make sure that alpha is smaller than 1.
    // Therefore we apply a "bending" transformation which almost preserves small values, but makes
    // sure that the result is within the expected interval.
    return t / (1.0 + t);
  }

  private State updateState(State oldState, long currentNanoTime, double delegateProbability) {
    double currentAverageProbability =
        oldState.effectiveDelegateProbability * (1.0 - probabilitySmoothingFactor)
            + delegateProbability * probabilitySmoothingFactor;

    long nanoTimeDelta = currentNanoTime - oldState.lastNanoTime;
    if (nanoTimeDelta <= 0.0) {
      // Low clock resolution or clock jumping backwards.
      // Assume time delta equal to zero.
      return new State(
          oldState.effectiveWindowCount + 1,
          oldState.effectiveWindowNanos,
          oldState.lastNanoTime,
          currentAverageProbability);
    }

    double decayFactor = Math.exp(-nanoTimeDelta * inverseAdaptationTimeNanos);
    double currentEffectiveWindowCount = oldState.effectiveWindowCount * decayFactor + 1;
    double currentEffectiveWindowNanos =
        oldState.effectiveWindowNanos * decayFactor + nanoTimeDelta;

    return new State(
        currentEffectiveWindowCount,
        currentEffectiveWindowNanos,
        currentNanoTime,
        currentAverageProbability);
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    double suggestedProbability;
    long suggestedThreshold;

    SamplingIntent delegateIntent =
        delegate.getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
    long delegateThreshold = delegateIntent.getThreshold();

    if (isValidThreshold(delegateThreshold)) {
      double delegateProbability = calculateSamplingProbability(delegateThreshold);
      long currentNanoTime = nanoTimeSupplier.getAsLong();
      State currentState =
          state.updateAndGet(s -> updateState(s, currentNanoTime, delegateProbability));

      double targetMaxProbability =
          (currentState.effectiveWindowNanos * targetSpansPerNanosecondLimit)
              / currentState.effectiveWindowCount;

      if (currentState.effectiveDelegateProbability > targetMaxProbability) {
        suggestedProbability =
            targetMaxProbability / currentState.effectiveDelegateProbability * delegateProbability;
      } else {
        suggestedProbability = delegateProbability;
      }
      suggestedThreshold = calculateThreshold(suggestedProbability);
    } else {
      suggestedThreshold = getInvalidThreshold();
    }

    return new SamplingIntent() {
      @Override
      public long getThreshold() {
        return suggestedThreshold;
      }

      @Override
      public Attributes getAttributes() {
        return delegateIntent.getAttributes();
      }

      @Override
      public TraceState updateTraceState(TraceState previousState) {
        return delegateIntent.updateTraceState(previousState);
      }
    };
  }

  @Override
  public String getDescription() {
    return description;
  }
}
