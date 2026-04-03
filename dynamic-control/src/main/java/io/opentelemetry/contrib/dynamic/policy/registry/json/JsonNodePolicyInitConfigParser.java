/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInitConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.ArrayList;
import java.util.List;

/** Shared JsonNode-to-model parser for registry init configuration. */
public final class JsonNodePolicyInitConfigParser {
  private JsonNodePolicyInitConfigParser() {}

  public static PolicyInitConfig parse(JsonNode root) {
    if (root == null) {
      throw new IllegalArgumentException("Config payload cannot be empty.");
    }
    JsonNode sourcesNode =
        requireArray(root.get("sources"), "Config must contain a 'sources' array.");
    List<PolicySourceConfig> sources = new ArrayList<>();
    for (JsonNode sourceNode : sourcesNode) {
      sources.add(parseSource(sourceNode));
    }
    return new PolicyInitConfig(sources);
  }

  private static PolicySourceConfig parseSource(JsonNode node) {
    JsonNode objectNode = requireObject(node, "Each source entry must be an object.");

    String kindValue =
        requireText(objectNode.get("kind"), "Each source must define string 'kind'.");
    String formatValue =
        requireText(objectNode.get("format"), "Each source must define string 'format'.");

    SourceKind kind = SourceKind.fromConfigValue(kindValue);
    SourceFormat format = SourceFormat.fromConfigValue(formatValue);

    JsonNode locationNode = objectNode.get("location");
    String location =
        locationNode != null && locationNode.isTextual() ? locationNode.asText() : null;

    JsonNode mappingsNode =
        requireArray(objectNode.get("mappings"), "Each source must define a 'mappings' array.");
    List<PolicySourceMappingConfig> mappings = new ArrayList<>();
    for (JsonNode mappingNode : mappingsNode) {
      mappings.add(parseMapping(mappingNode));
    }
    return new PolicySourceConfig(kind, format, location, mappings);
  }

  private static PolicySourceMappingConfig parseMapping(JsonNode node) {
    JsonNode objectNode = requireObject(node, "Each mapping entry must be an object.");
    String sourceKey =
        requireText(objectNode.get("sourceKey"), "Each mapping must define string 'sourceKey'.");
    String policyType =
        requireText(objectNode.get("policyType"), "Each mapping must define string 'policyType'.");
    return new PolicySourceMappingConfig(sourceKey, policyType);
  }

  @CanIgnoreReturnValue
  private static JsonNode requireArray(JsonNode node, String message) {
    if (node == null || !node.isArray()) {
      throw new IllegalArgumentException(message);
    }
    return node;
  }

  @CanIgnoreReturnValue
  private static JsonNode requireObject(JsonNode node, String message) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException(message);
    }
    return node;
  }

  private static String requireText(JsonNode node, String message) {
    if (node == null || !node.isTextual()) {
      throw new IllegalArgumentException(message);
    }
    String value = node.asText();
    if (value.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }
}
