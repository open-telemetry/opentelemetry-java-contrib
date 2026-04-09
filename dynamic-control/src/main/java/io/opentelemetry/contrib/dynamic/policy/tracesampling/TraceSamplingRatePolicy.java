/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import javax.annotation.Nullable;

public final class TraceSamplingRatePolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "trace-sampling";

  @Nullable private static volatile DelegatingSampler initializedSampler;

  private final double probability;

  public TraceSamplingRatePolicy(double probability) {
    super(POLICY_TYPE);
    this.probability = normalizeProbability(probability);
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
  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    Sampler initialDelegate = createSampler(1.0);
    DelegatingSampler delegatingSampler = new DelegatingSampler(initialDelegate);
    initializedSampler = delegatingSampler;
    autoConfiguration.addSamplerCustomizer((sampler, config) -> delegatingSampler);
  }

  /**
   * Creates the composed sampler used for this policy probability.
   *
   * @param probability sampling probability in the inclusive range {@code [0.0, 1.0]}
   * @return a sampler equivalent to the configured probability with parent-based behavior
   * @throws IllegalArgumentException if probability is NaN or outside {@code [0.0, 1.0]}
   */
  public static Sampler createSampler(double probability) {
    probability = normalizeProbability(probability);
    return CompositeSampler.wrap(
        ComposableSampler.parentThreshold(ComposableSampler.probability(probability)));
  }

  private static double normalizeProbability(double probability) {
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    // Normalize -0.0 to +0.0 so equality/hash behavior stays intuitive.
    return probability == 0.0 ? 0.0 : probability;
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return initializedSampler;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TraceSamplingRatePolicy)) {
      return false;
    }
    TraceSamplingRatePolicy that = (TraceSamplingRatePolicy) obj;
    return Double.compare(probability, that.probability) == 0;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(probability);
  }
}
