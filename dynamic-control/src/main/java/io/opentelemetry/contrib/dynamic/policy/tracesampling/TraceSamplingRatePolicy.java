/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicyIdentity;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import javax.annotation.Nullable;

public final class TraceSamplingRatePolicy extends AbstractTraceSamplingPolicy {
  public static final String POLICY_TYPE = "trace-sampling";
  public static final TelemetryPolicyIdentity DEFAULT_IDENTITY =
      new TelemetryPolicyIdentity("trace-sampling", "Trace sampling rate");

  private final double probability;

  public TraceSamplingRatePolicy(double probability, SourceKind sourceKind) {
    super(DEFAULT_IDENTITY, probability, sourceKind);
    this.probability = getSamplingProbability();
  }

  @Override
  public String getType() {
    return POLICY_TYPE;
  }

  public double getProbability() {
    return probability;
  }

  /**
   * Initializes runtime wiring for this policy type.
   *
   * <p>If the extension is configured to use this policy, this installs an opinionated sampler that
   * overrides any other sampler
   */
  public static PolicyImplementer initialize(AutoConfigurationCustomizer autoConfiguration) {
    return AbstractTraceSamplingPolicy.initialize(autoConfiguration);
  }

  public static void registerPolicyType() {
    PolicyInit.registerPolicyType(
        POLICY_TYPE, TraceSamplingRatePolicy.class, TraceSamplingRatePolicy::initialize);
  }

  /**
   * Creates the composed sampler used for this policy probability.
   *
   * @param probability sampling probability in the inclusive range {@code [0.0, 1.0]}
   * @return a sampler equivalent to the configured probability with parent-based behavior
   * @throws IllegalArgumentException if probability is NaN or outside {@code [0.0, 1.0]}
   */
  public static Sampler createSampler(double probability) {
    return AbstractTraceSamplingPolicy.createSampler(probability);
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return AbstractTraceSamplingPolicy.getInitializedSampler();
  }

  static void resetForTest() {
    AbstractTraceSamplingPolicy.resetForTest();
  }
}
