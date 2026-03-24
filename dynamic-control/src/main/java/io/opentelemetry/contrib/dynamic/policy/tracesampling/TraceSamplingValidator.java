/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Validator for trace sampling policies.
 *
 * <p>This validator handles the "trace-sampling" policy type.
 */
public final class TraceSamplingValidator extends AbstractSourcePolicyValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingValidator.class.getName());

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateJsonValue(JsonNode valueNode) {
    JsonNode probabilityNode = valueNode;
    if (valueNode.isObject()) {
      probabilityNode = valueNode.get("probability");
      if (probabilityNode == null) {
        return null;
      }
    }
    Double probability = parseDouble(probabilityNode);
    if (probability == null) {
      return null;
    }
    return createPolicy(probability);
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateKeyValueValue(String value) {
    Double probability = parseDouble(value);
    if (probability == null) {
      return null;
    }
    return createPolicy(probability);
  }

  @Nullable
  private static TelemetryPolicy createPolicy(double probability) {
    try {
      return new TraceSamplingRatePolicy(probability);
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
