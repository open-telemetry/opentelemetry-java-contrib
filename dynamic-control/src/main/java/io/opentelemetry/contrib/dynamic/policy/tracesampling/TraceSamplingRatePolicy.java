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

/** Trace sampling policy expressed as a ratio in the inclusive range {@code [0.0, 1.0]}. */
public final class TraceSamplingRatePolicy extends AbstractTraceSamplingPolicy {
  public static final String POLICY_TYPE = "trace-sampling";
  public static final TelemetryPolicyIdentity DEFAULT_IDENTITY =
      new TelemetryPolicyIdentity("trace-sampling", "Trace sampling rate");

  public TraceSamplingRatePolicy(double ratio, SourceKind sourceKind) {
    super(DEFAULT_IDENTITY, normalizeRatio(ratio), sourceKind);
  }

  @Override
  public String getType() {
    return POLICY_TYPE;
  }

  public double getRatio() {
    return getSamplingProbability();
  }

  public double getProbability() {
    return getSamplingProbability();
  }

  /**
   * Initializes runtime wiring for this policy type.
   *
   * <p>If the extension is configured to use this policy, this installs an opinionated sampler that
   * overrides any other sampler
   */
  public static PolicyImplementer initialize(AutoConfigurationCustomizer autoConfiguration) {
    return initialize(autoConfiguration, new TraceSamplingValidator());
  }

  public static void registerPolicyType() {
    PolicyInit.registerPolicyType(
        POLICY_TYPE, TraceSamplingRatePolicy.class, TraceSamplingRatePolicy::initialize);
  }

  /**
   * Creates the composed sampler used for this policy ratio.
   *
   * @param ratio sampling ratio (sampling probability) in the inclusive range {@code [0.0, 1.0]}
   * @return a sampler equivalent to the configured ratio with parent-based behavior
   * @throws IllegalArgumentException if ratio is NaN or outside {@code [0.0, 1.0]}
   */
  public static Sampler createSampler(double ratio) {
    return AbstractTraceSamplingPolicy.createSampler(ratio);
  }

  private static double normalizeRatio(double ratio) {
    if (Double.isNaN(ratio) || ratio < 0.0 || ratio > 1.0) {
      throw new IllegalArgumentException("ratio must be within [0.0, 1.0]");
    }
    return ratio == 0.0 ? 0.0 : ratio;
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return AbstractTraceSamplingPolicy.getInitializedSampler();
  }

  static void resetForTest() {
    AbstractTraceSamplingPolicy.resetForTest();
  }
}
