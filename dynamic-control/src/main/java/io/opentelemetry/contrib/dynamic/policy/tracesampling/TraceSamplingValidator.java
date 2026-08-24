/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Validator for trace sampling policies.
 *
 * <p>This validator handles the "trace-sampling" policy type.
 */
public final class TraceSamplingValidator extends AbstractTraceSamplingValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingValidator.class.getName());

  public TraceSamplingValidator() {
    super("probability");
  }

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy createPolicy(double probability, SourceKind sourceKind) {
    try {
      return new TraceSamplingRatePolicy(probability, sourceKind);
    } catch (IllegalArgumentException e) {
      logger.info(
          "Invalid trace-sampling probability '"
              + probability
              + "' will be ignored: "
              + e.getMessage());
      return null;
    }
  }
}
