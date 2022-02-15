/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.internal.GuardedBy;
import io.opentelemetry.contrib.util.DefaultRandomGenerator;
import io.opentelemetry.contrib.util.RandomGenerator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.function.LongSupplier;

/**
 * This consistent {@link Sampler} adjust the sampling probability dynamically to limit the rate of
 * sampled spans.
 *
 * <p>This sampler uses exponential smoothing to estimate on irregular data (compare Wright, David
 * J. "Forecasting data published at irregular time intervals using an extension of Holt's method."
 * Management science 32.4 (1986): 499-510.) to estimate the current rate of spans.
 */
final class ConsistentRateLimitingSampler extends ConsistentSampler {

  private final String description;
  private final LongSupplier nanoTimeSupplier;
  private final double inverseAdaptationTimeNanos;
  private final double targetSpansPerNanosLimit;

  @GuardedBy("this")
  private double effectiveWindowCount;

  @GuardedBy("this")
  private double effectiveWindowNanos;

  @GuardedBy("this")
  private long lastNanoTime;

  /**
   * Constructor.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   */
  ConsistentRateLimitingSampler(double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    this(
        targetSpansPerSecondLimit,
        adaptationTimeSeconds,
        DefaultRandomGenerator.get(),
        System::nanoTime);
  }

  /**
   * Constructor.
   *
   * @param targetSpansPerSecondLimit the desired spans per second limit
   * @param adaptationTimeSeconds the typical time to adapt to a new load (time constant used for
   *     exponential smoothing)
   * @param threadSafeRandomGenerator a thread-safe random generator
   * @param nanoTimeSupplier a supplier for the current nano time
   */
  ConsistentRateLimitingSampler(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      RandomGenerator threadSafeRandomGenerator,
      LongSupplier nanoTimeSupplier) {
    super(threadSafeRandomGenerator);

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
    this.targetSpansPerNanosLimit = 1e-9 * targetSpansPerSecondLimit;

    synchronized (this) {
      this.effectiveWindowCount = 0;
      this.effectiveWindowNanos = 0;
      this.lastNanoTime = nanoTimeSupplier.getAsLong();
    }
  }

  private synchronized double updateAndGetSamplingProbability() {
    long currentNanoTime = Math.max(nanoTimeSupplier.getAsLong(), lastNanoTime);
    long nanoTimeDelta = currentNanoTime - lastNanoTime;
    lastNanoTime = currentNanoTime;
    double decayFactor = Math.exp(-nanoTimeDelta * inverseAdaptationTimeNanos);
    effectiveWindowCount = effectiveWindowCount * decayFactor + 1;
    effectiveWindowNanos = effectiveWindowNanos * decayFactor + nanoTimeDelta;
    return Math.min(1., (effectiveWindowNanos * targetSpansPerNanosLimit) / effectiveWindowCount);
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    double samplingProbability = updateAndGetSamplingProbability();

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

    if (threadSafeRandomGenerator.nextBoolean(probabilityToUseLowerPValue)) {
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
