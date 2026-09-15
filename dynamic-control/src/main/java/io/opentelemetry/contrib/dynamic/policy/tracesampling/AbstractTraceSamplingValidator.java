/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import javax.annotation.Nullable;

/** Shared parsing for numeric trace sampling policy values. */
abstract class AbstractTraceSamplingValidator extends AbstractSourcePolicyValidator {
  private final String valueKeyword;

  AbstractTraceSamplingValidator(String valueKeyword) {
    this.valueKeyword = valueKeyword;
  }

  @Override
  @Nullable
  protected final TelemetryPolicy validateJsonValue(JsonNode valueNode, SourceKind sourceKind) {
    JsonNode numericNode = valueNode;
    if (valueNode.isObject()) {
      numericNode = valueNode.get(valueKeyword);
      if (numericNode == null) {
        return null;
      }
    }
    Double value = parseDouble(numericNode);
    return value == null ? null : createPolicy(value, sourceKind);
  }

  @Override
  @Nullable
  protected final TelemetryPolicy validateKeyValueValue(String value, SourceKind sourceKind) {
    Double numericValue = parseDouble(value);
    return numericValue == null ? null : createPolicy(numericValue, sourceKind);
  }

  @Nullable
  protected abstract TelemetryPolicy createPolicy(double value, SourceKind sourceKind);
}
