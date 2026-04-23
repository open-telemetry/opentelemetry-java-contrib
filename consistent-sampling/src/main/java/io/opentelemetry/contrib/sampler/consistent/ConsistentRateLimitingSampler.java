/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.INVALID_THRESHOLD;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.isValidThreshold;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent rate-limiting sampler that adjusts the effective sampling probability dynamically to
 * meet a target maximum number of sampled spans per second.
 *
 * <p>The implementation uses exponential smoothing for the estimation of the effective sampling
 * probability and the effective span rate.
 */
final class ConsistentRateLimitingSampler implements ComposableSampler {

  private static final double NANOS_IN_SECONDS = 1e-9;

  @Immutable
  private static final class State {
    private final double effectiveWindowCount;
    private final double effectiveWindowNanos;
    private final double effectiveDelegateProbability;
    private final long lastNanoTime;

    State(
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
    double t = 1.0 / (targetSpansPerSecondLimit * adaptationTimeSeconds);
    return t / (1.0 + t);
  }

  private State updateState(State oldState, long currentNanoTime, double delegateProbability) {
    double currentAverageProbability =
        oldState.effectiveDelegateProbability * (1.0 - probabilitySmoothingFactor)
            + delegateProbability * probabilitySmoothingFactor;

    long nanoTimeDelta = currentNanoTime - oldState.lastNanoTime;
    if (nanoTimeDelta <= 0.0) {
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
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SamplingIntent delegateIntent =
        delegate.getSamplingIntent(parentContext, traceId, name, spanKind, attributes, parentLinks);
    long delegateThreshold = delegateIntent.getThreshold();

    long suggestedThreshold;
    if (isValidThreshold(delegateThreshold)) {
      double delegateProbability = calculateSamplingProbability(delegateThreshold);
      long currentNanoTime = nanoTimeSupplier.getAsLong();
      State currentState =
          state.updateAndGet(s -> updateState(s, currentNanoTime, delegateProbability));

      double targetMaxProbability =
          (currentState.effectiveWindowNanos * targetSpansPerNanosecondLimit)
              / currentState.effectiveWindowCount;

      double suggestedProbability;
      if (currentState.effectiveDelegateProbability > targetMaxProbability) {
        suggestedProbability =
            targetMaxProbability / currentState.effectiveDelegateProbability * delegateProbability;
      } else {
        suggestedProbability = delegateProbability;
      }
      suggestedThreshold = calculateThreshold(suggestedProbability);
    } else {
      suggestedThreshold = INVALID_THRESHOLD;
    }

    return SamplingIntent.create(
        suggestedThreshold,
        delegateIntent.isThresholdReliable(),
        delegateIntent.getAttributes(),
        delegateIntent.getTraceStateUpdater());
  }

  @Override
  public String getDescription() {
    return description;
  }
}
