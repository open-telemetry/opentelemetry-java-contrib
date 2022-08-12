/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;
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
 */
final class ConsistentRateLimitingSampler extends ConsistentSampler {

  @Immutable
  private static final class State {
    private final double effectiveWindowCount;
    private final double effectiveWindowNanos;
    private final long lastNanoTime;

    public State(double effectiveWindowCount, double effectiveWindowNanos, long lastNanoTime) {
      this.effectiveWindowCount = effectiveWindowCount;
      this.effectiveWindowNanos = effectiveWindowNanos;
      this.lastNanoTime = lastNanoTime;
    }
  }

  private final String description;
  private final LongSupplier nanoTimeSupplier;
  private final double inverseAdaptationTimeNanos;
  private final double targetSpansPerNanosecondLimit;
  private final AtomicReference<State> state;
  private final RandomGenerator randomGenerator;

  /**
   * Constructor.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param rValueGenerator the function to use for generating the r-value
   * @param randomGenerator a random generator
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  ConsistentRateLimitingSampler(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      ToIntFunction<String> rValueGenerator,
      RandomGenerator randomGenerator,
      LongSupplier nanoTimeSupplier) {
    super(rValueGenerator);

    if (targetSpansPerSecondLimit < 0.0) {
      throw new IllegalArgumentException("Limit for sampled spans per second must be nonnegative!");
    }
    if (adaptationTimeSeconds < 0.0) {
      throw new IllegalArgumentException("Adaptation rate must be nonnegative!");
    }
    this.description =
        String.format(
            "ConsistentRateLimitingSampler{%.6f, %.6f}",
            targetSpansPerSecondLimit, adaptationTimeSeconds);
    this.nanoTimeSupplier = requireNonNull(nanoTimeSupplier);

    this.inverseAdaptationTimeNanos = 1e-9 / adaptationTimeSeconds;
    this.targetSpansPerNanosecondLimit = 1e-9 * targetSpansPerSecondLimit;

    this.state = new AtomicReference<>(new State(0, 0, nanoTimeSupplier.getAsLong()));

    this.randomGenerator = randomGenerator;
  }

  private State updateState(State oldState, long currentNanoTime) {
    if (currentNanoTime <= oldState.lastNanoTime) {
      return new State(
          oldState.effectiveWindowCount + 1, oldState.effectiveWindowNanos, oldState.lastNanoTime);
    }
    long nanoTimeDelta = currentNanoTime - oldState.lastNanoTime;
    double decayFactor = Math.exp(-nanoTimeDelta * inverseAdaptationTimeNanos);
    double currentEffectiveWindowCount = oldState.effectiveWindowCount * decayFactor + 1;
    double currentEffectiveWindowNanos =
        oldState.effectiveWindowNanos * decayFactor + nanoTimeDelta;
    return new State(currentEffectiveWindowCount, currentEffectiveWindowNanos, currentNanoTime);
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    long currentNanoTime = nanoTimeSupplier.getAsLong();
    State currentState = state.updateAndGet(s -> updateState(s, currentNanoTime));

    double samplingProbability =
        (currentState.effectiveWindowNanos * targetSpansPerNanosecondLimit)
            / currentState.effectiveWindowCount;

    if (samplingProbability >= 1.) {
      return 0;
    }

    int lowerPValue = getLowerBoundP(samplingProbability);
    int upperPValue = getUpperBoundP(samplingProbability);

    if (lowerPValue == upperPValue) {
      return lowerPValue;
    }

    double upperSamplingRate = getSamplingProbability(lowerPValue);
    double lowerSamplingRate = getSamplingProbability(upperPValue);
    double probabilityToUseLowerPValue =
        (samplingProbability - lowerSamplingRate) / (upperSamplingRate - lowerSamplingRate);

    if (randomGenerator.nextBoolean(probabilityToUseLowerPValue)) {
      return lowerPValue;
    } else {
      return upperPValue;
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
