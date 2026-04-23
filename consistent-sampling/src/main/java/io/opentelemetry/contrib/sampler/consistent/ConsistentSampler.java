/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import java.util.function.LongSupplier;

/**
 * Factory entry points for the contrib-only consistent probability samplers that are not part of
 * the upstream {@link ComposableSampler} API.
 *
 * <ul>
 *   <li>{@link ConsistentRateLimitingSampler} &mdash; adaptive rate limiting
 *   <li>{@link ConsistentVariableThresholdSampler} &mdash; fixed probability that can be updated at
 *       runtime
 *   <li>{@link ConsistentAnyOf} &mdash; the minimum-threshold combination of several composable
 *       samplers
 * </ul>
 *
 * <p>For the common samplers (always-on/off, fixed probability, parent-based, rule-based,
 * annotating) use {@link ComposableSampler}'s static factories directly. To turn a {@link
 * ComposableSampler} into a {@link io.opentelemetry.sdk.trace.samplers.Sampler} use {@link
 * io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler#wrap(ComposableSampler)}.
 */
public final class ConsistentSampler {

  private ConsistentSampler() {}

  /**
   * Returns a {@link ComposableSampler} that attempts to adjust the sampling probability
   * dynamically to meet the target span rate. Spans are first passed to {@link
   * ComposableSampler#alwaysOn()} and then rate-limited.
   */
  public static ComposableSampler rateLimited(
      double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return rateLimited(
        ComposableSampler.alwaysOn(), targetSpansPerSecondLimit, adaptationTimeSeconds);
  }

  /**
   * Returns a {@link ComposableSampler} that honors the delegate's sampling decision as long as it
   * seems to meet the target span rate. In case the delegate's sampling rate seems to exceed the
   * target, the sampler attempts to decrease the effective sampling probability dynamically.
   */
  @SuppressWarnings("InconsistentOverloads")
  public static ComposableSampler rateLimited(
      ComposableSampler delegate, double targetSpansPerSecondLimit, double adaptationTimeSeconds) {
    return rateLimited(
        delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, System::nanoTime);
  }

  // Package-private overloads exposing the nanoTimeSupplier for tests.

  static ComposableSampler rateLimited(
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier) {
    return rateLimited(
        ComposableSampler.alwaysOn(),
        targetSpansPerSecondLimit,
        adaptationTimeSeconds,
        nanoTimeSupplier);
  }

  @SuppressWarnings("InconsistentOverloads")
  static ComposableSampler rateLimited(
      ComposableSampler delegate,
      double targetSpansPerSecondLimit,
      double adaptationTimeSeconds,
      LongSupplier nanoTimeSupplier) {
    return new ConsistentRateLimitingSampler(
        delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);
  }

  /**
   * Returns a {@link ComposableSampler} with a fixed sampling probability that can be updated at
   * runtime via {@link ConsistentVariableThresholdSampler#setSamplingProbability(double)}.
   */
  public static ConsistentVariableThresholdSampler updateableProbabilityBased(
      double samplingProbability) {
    return new ConsistentVariableThresholdSampler(samplingProbability);
  }

  /**
   * Returns a {@link ComposableSampler} that queries all its delegates for their sampling
   * threshold. The intention is to make a positive sampling decision if any of the delegates would
   * make a positive decision. The returned sampler uses the minimum threshold value found among all
   * delegates.
   *
   * @param delegates the delegate samplers, at least one delegate must be specified
   */
  public static ComposableSampler anyOf(ComposableSampler... delegates) {
    return new ConsistentAnyOf(delegates);
  }
}
