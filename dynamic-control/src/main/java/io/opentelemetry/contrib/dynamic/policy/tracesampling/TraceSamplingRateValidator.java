/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Validator for {@code sampling-rate} policies expressed as ratios. */
public final class TraceSamplingRateValidator extends AbstractTraceSamplingValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingRateValidator.class.getName());

  public TraceSamplingRateValidator() {
    super("ratio");
  }

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy createPolicy(double ratio, SourceKind sourceKind) {
    try {
      return new TraceSamplingRatePolicy(ratio, sourceKind);
    } catch (IllegalArgumentException e) {
      logger.info("Invalid sampling-rate ratio '" + ratio + "' will be ignored: " + e.getMessage());
      return null;
    }
  }
}
