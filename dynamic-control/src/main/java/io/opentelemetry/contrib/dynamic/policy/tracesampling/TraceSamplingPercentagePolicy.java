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
import javax.annotation.Nullable;

/** Trace sampling policy expressed as a percentage in the inclusive range {@code [0.0, 100.0]}. */
public final class TraceSamplingPercentagePolicy extends AbstractTraceSamplingPolicy {
  public static final String POLICY_TYPE = "trace-sampling";
  public static final TelemetryPolicyIdentity DEFAULT_IDENTITY =
      new TelemetryPolicyIdentity("trace-sampling", "Trace sampling percentage");

  public TraceSamplingPercentagePolicy(double percentage, SourceKind sourceKind) {
    super(DEFAULT_IDENTITY, normalizePercentage(percentage) / 100.0, sourceKind);
  }

  @Override
  public String getType() {
    return POLICY_TYPE;
  }

  public double getPercentage() {
    return getSamplingProbability() * 100.0;
  }

  public static PolicyImplementer initialize(AutoConfigurationCustomizer autoConfiguration) {
    return initialize(autoConfiguration, new TraceSamplingPercentageValidator());
  }

  public static void registerPolicyType() {
    PolicyInit.registerPolicyType(
        POLICY_TYPE,
        TraceSamplingPercentagePolicy.class,
        TraceSamplingPercentagePolicy::initialize);
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return AbstractTraceSamplingPolicy.getInitializedSampler();
  }

  static void resetForTest() {
    AbstractTraceSamplingPolicy.resetForTest();
  }

  private static double normalizePercentage(double percentage) {
    if (Double.isNaN(percentage) || percentage < 0.0 || percentage > 100.0) {
      throw new IllegalArgumentException("percentage must be within [0.0, 100.0]");
    }
    return percentage == 0.0 ? 0.0 : percentage;
  }
}
