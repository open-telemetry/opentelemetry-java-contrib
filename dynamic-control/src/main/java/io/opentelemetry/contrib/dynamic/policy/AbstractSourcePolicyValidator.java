/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.KeyValueSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import javax.annotation.Nullable;

/** Base validator with common source-dispatch and parse helpers. */
public abstract class AbstractSourcePolicyValidator implements PolicyValidator {

  @Override
  @Nullable
  public final TelemetryPolicy validate(SourceWrapper source) {
    if (source == null) {
      return null;
    }
    SourceFormat format = source.getFormat();
    switch (format) {
      case JSON: // transitional: same payload shape as JSONKEYVALUE until JSON is removed
      case JSONKEYVALUE:
        return validateJsonSource(((JsonSourceWrapper) source).asJsonNode());
      case KEYVALUE:
        return validateKeyValueSource((KeyValueSourceWrapper) source);
    }
    return null;
  }

  @Nullable
  private TelemetryPolicy validateJsonSource(JsonNode node) {
    JsonNode valueNode = node.get(getPolicyType());
    if (valueNode == null) {
      return null;
    }
    return validateJsonValue(valueNode);
  }

  @Nullable
  private TelemetryPolicy validateKeyValueSource(KeyValueSourceWrapper source) {
    if (!getPolicyType().equals(source.getKey().trim())) {
      return null;
    }
    return validateKeyValueValue(source.getValue());
  }

  @Nullable
  protected abstract TelemetryPolicy validateJsonValue(JsonNode valueNode);

  @Nullable
  protected abstract TelemetryPolicy validateKeyValueValue(String value);

  @Nullable
  protected static Double parseDouble(JsonNode valueNode) {
    if (valueNode.isNumber()) {
      return valueNode.asDouble();
    }
    if (valueNode.isTextual()) {
      return parseDouble(valueNode.asText());
    }
    return null;
  }

  @Nullable
  protected static Double parseDouble(String value) {
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Nullable
  protected static Boolean parseBoolean(JsonNode valueNode) {
    if (valueNode.isBoolean()) {
      return valueNode.asBoolean();
    }
    if (valueNode.isTextual()) {
      return parseBoolean(valueNode.asText());
    }
    return null;
  }

  @Nullable
  protected static Boolean parseBoolean(String value) {
    String normalized = value.trim();
    if ("true".equalsIgnoreCase(normalized)) {
      return true;
    }
    if ("false".equalsIgnoreCase(normalized)) {
      return false;
    }
    return null;
  }
}
