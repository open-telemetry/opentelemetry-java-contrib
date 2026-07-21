/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.KeyValueSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Converts parsed source policy entries into validated telemetry policies using source mappings.
 */
final class MappedPolicySourceConverter {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Map<String, String> policyIdToPolicyType;
  private final List<PolicyValidator> validators;

  private MappedPolicySourceConverter(
      Map<String, String> policyIdToPolicyType, List<PolicyValidator> validators) {
    this.policyIdToPolicyType =
        Collections.unmodifiableMap(
            new HashMap<>(
                Objects.requireNonNull(
                    policyIdToPolicyType, "policyIdToPolicyType cannot be null")));
    this.validators =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(validators, "validators cannot be null")));
  }

  static MappedPolicySourceConverter create(
      List<PolicySourceMappingConfig> mappings, List<PolicyValidator> validators) {
    return new MappedPolicySourceConverter(buildPolicyIdToPolicyType(mappings), validators);
  }

  Set<String> getMappedPolicyIds() {
    return policyIdToPolicyType.keySet();
  }

  List<TelemetryPolicy> convert(List<SourceWrapper> sources, SourceKind sourceKind) {
    Objects.requireNonNull(sources, "sources cannot be null");
    Objects.requireNonNull(sourceKind, "sourceKind cannot be null");
    List<TelemetryPolicy> policies = new ArrayList<>();
    for (SourceWrapper source : sources) {
      TelemetryPolicy policy = convert(source, sourceKind);
      if (policy != null) {
        policies.add(policy);
      }
    }
    return policies;
  }

  @Nullable
  TelemetryPolicy convert(SourceWrapper source, SourceKind sourceKind) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(sourceKind, "sourceKind cannot be null");
    String incomingPolicyId = source.getPolicyType();
    if (incomingPolicyId == null || incomingPolicyId.isEmpty()) {
      return null;
    }
    String policyType = policyIdToPolicyType.get(incomingPolicyId);
    if (policyType == null) {
      return null;
    }
    SourceWrapper normalizedSource = remapSourcePolicyType(source, policyType);
    if (normalizedSource == null) {
      return null;
    }
    for (PolicyValidator validator : validators) {
      if (!policyType.equals(validator.getPolicyType())) {
        continue;
      }
      TelemetryPolicy policy = validator.validate(normalizedSource, sourceKind);
      if (policy != null) {
        return policy;
      }
    }
    return null;
  }

  private static Map<String, String> buildPolicyIdToPolicyType(
      List<PolicySourceMappingConfig> mappings) {
    Objects.requireNonNull(mappings, "mappings cannot be null");
    Map<String, String> mapping = new HashMap<>();
    for (PolicySourceMappingConfig item : mappings) {
      mapping.put(item.getPolicyId(), item.getPolicyType());
    }
    return mapping;
  }

  @Nullable
  private static SourceWrapper remapSourcePolicyType(
      SourceWrapper source, String mappedPolicyType) {
    if (source instanceof JsonSourceWrapper) {
      JsonNode node = ((JsonSourceWrapper) source).asJsonNode();
      if (!node.isObject() || node.size() != 1) {
        return null;
      }
      JsonNode value = node.elements().next();
      ObjectNode remappedNode = MAPPER.createObjectNode();
      remappedNode.set(mappedPolicyType, value);
      return new JsonSourceWrapper(remappedNode);
    }
    if (source instanceof KeyValueSourceWrapper) {
      KeyValueSourceWrapper keyValue = (KeyValueSourceWrapper) source;
      return new KeyValueSourceWrapper(mappedPolicyType, keyValue.getValue());
    }
    return source;
  }
}
