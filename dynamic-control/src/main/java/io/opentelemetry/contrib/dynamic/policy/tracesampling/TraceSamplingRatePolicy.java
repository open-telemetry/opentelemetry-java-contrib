/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.Objects;

public final class TraceSamplingRatePolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "trace-sampling";

  private final double probability;

  public TraceSamplingRatePolicy(double probability) {
    super(POLICY_TYPE);
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    this.probability = probability;
  }

  public double getProbability() {
    return probability;
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
    return Objects.hash(probability);
  }
}
