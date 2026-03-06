/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Validator for trace sampling policies.
 *
 * <p>This validator handles the "trace-sampling" policy type and supports the
 * "trace-sampling.probability" alias.
 */
public final class TraceSamplingValidator implements PolicyValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingValidator.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.TYPE;
  }

  @Override
  public String getAlias() {
    return "trace-sampling.probability";
  }

  @Override
  @Nullable
  public TelemetryPolicy validate(String json) {
    try {
      JsonNode node = MAPPER.readTree(json);
      if (node.has(getPolicyType())) {
        JsonNode spec = node.get(getPolicyType());
        if (spec.has("probability")) {
          JsonNode probNode = spec.get("probability");
          if (probNode.isNumber()) {
            double d = probNode.asDouble();
            if (d >= 0.0 && d <= 1.0) {
              return new TraceSamplingRatePolicy(d);
            }
          }
        }
      }
    } catch (JsonProcessingException e) {
      // Not valid JSON for this validator
    }
    logger.info("Invalid trace-sampling JSON: " + json);
    return null;
  }

  @Override
  @Nullable
  public TelemetryPolicy validateAlias(String key, String value) {
    if (getAlias() != null && getAlias().equals(key)) {
      try {
        double d = Double.parseDouble(value);
        if (d >= 0.0 && d <= 1.0) {
          return new TraceSamplingRatePolicy(d);
        }
      } catch (NumberFormatException e) {
        // invalid
      }
      logger.info("Ignoring invalid trace-sampling.probability value: " + value);
    }
    return null;
  }
}
