/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicyIdentity;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import javax.annotation.Nullable;

/** Shared representation and runtime wiring for trace sampling policies. */
public abstract class AbstractTraceSamplingPolicy implements TelemetryPolicy {
  @Nullable private static volatile DelegatingSampler initializedSampler;

  private final TelemetryPolicyIdentity identity;
  private final double samplingProbability;
  private final SourceKind sourceKind;

  protected AbstractTraceSamplingPolicy(
      TelemetryPolicyIdentity identity, double samplingProbability, SourceKind sourceKind) {
    this.identity = Objects.requireNonNull(identity, "identity cannot be null");
    this.samplingProbability = normalizeProbability(samplingProbability);
    this.sourceKind = Objects.requireNonNull(sourceKind, "sourceKind cannot be null");
  }

  @Override
  public final TelemetryPolicyIdentity getIdentity() {
    return identity;
  }

  @Override
  public final SourceKind getSourceKind() {
    return sourceKind;
  }

  /** Returns this policy value converted to an SDK sampling probability in {@code [0.0, 1.0]}. */
  public final double getSamplingProbability() {
    return samplingProbability;
  }

  protected static PolicyImplementer initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    DelegatingSampler delegatingSampler = new DelegatingSampler(createSampler(1.0));
    initializedSampler = delegatingSampler;
    autoConfiguration.addSamplerCustomizer((sampler, config) -> delegatingSampler);
    return new TraceSamplingRatePolicyImplementer(delegatingSampler);
  }

  /**
   * Creates the composed sampler used for a normalized sampling probability.
   *
   * @param probability sampling probability in the inclusive range {@code [0.0, 1.0]}
   */
  public static Sampler createSampler(double probability) {
    probability = normalizeProbability(probability);
    return CompositeSampler.wrap(
        ComposableSampler.parentThreshold(ComposableSampler.probability(probability)));
  }

  protected static double normalizeProbability(double probability) {
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    // handle -0.0
    return probability == 0.0 ? 0.0 : probability;
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return initializedSampler;
  }

  static void resetForTest() {
    initializedSampler = null;
  }
}
