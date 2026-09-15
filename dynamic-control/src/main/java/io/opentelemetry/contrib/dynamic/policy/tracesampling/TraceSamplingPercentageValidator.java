/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Validator for {@code trace-sampling} policies expressed as percentages. */
public final class TraceSamplingPercentageValidator extends AbstractTraceSamplingValidator {
  private static final Logger logger =
      Logger.getLogger(TraceSamplingPercentageValidator.class.getName());

  public TraceSamplingPercentageValidator() {
    super("percentage");
  }

  @Override
  public String getPolicyType() {
    return TraceSamplingPercentagePolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy createPolicy(double percentage, SourceKind sourceKind) {
    try {
      return new TraceSamplingPercentagePolicy(percentage, sourceKind);
    } catch (IllegalArgumentException e) {
      logger.info(
          "Invalid trace-sampling percentage '"
              + percentage
              + "' will be ignored: "
              + e.getMessage());
      return null;
    }
  }
}
