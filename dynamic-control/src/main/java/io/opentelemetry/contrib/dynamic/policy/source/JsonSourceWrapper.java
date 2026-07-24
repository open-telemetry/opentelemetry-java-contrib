/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Source wrapper for one policy parsed from JSON text that matches the {@link
 * SourceFormat#JSONKEYVALUE} shape. Each wrapper contains exactly one top-level key (the policy
 * type) and one value (the payload).
 */
public final class JsonSourceWrapper implements SourceWrapper {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final JsonNode source;

  public JsonSourceWrapper(JsonNode source) {
    this.source = Objects.requireNonNull(source, "source cannot be null");
  }

  @Override
  public SourceFormat getFormat() {
    return SourceFormat.JSONKEYVALUE;
  }

  @Override
  @Nullable
  public String getPolicyType() {
    JsonNode node = asJsonNode();
    if (!node.isObject() || node.size() != 1) {
      return null;
    }
    return node.fieldNames().next();
  }

  public JsonNode asJsonNode() {
    return source;
  }

  /**
   * Parses JSON text into one wrapper per policy object.
   *
   * <p>Input must be a JSON object containing policy ID/value pairs, or an array of single-policy
   * objects. Multi-policy objects are split into one wrapper per entry. Entries whose policy ID is
   * not present in {@code mappedPolicyIds}, or whose shape is invalid, are skipped while valid
   * entries continue through the pipeline. An empty JSON object or array yields an empty list.
   *
   * @return an empty list if the source contains no valid mapped policies; a non-empty list of
   *     wrappers for valid mapped policies; or {@code null} if the text is not valid JSON or its
   *     root is neither an object nor an array
   * @param mappedPolicyIds configured policy IDs accepted as JSON object keys
   * @throws NullPointerException if source or mappedPolicyIds is null
   */
  @Nullable
  public static List<SourceWrapper> parse(String source, Set<String> mappedPolicyIds) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(mappedPolicyIds, "mappedPolicyIds cannot be null");
    try {
      JsonNode parsed = MAPPER.readTree(source);
      if (parsed.isObject()) {
        return wrapMappedObject(parsed, mappedPolicyIds);
      }
      if (parsed.isArray()) {
        return wrapMappedArray(parsed, mappedPolicyIds);
      }
      return null;
    } catch (JsonProcessingException e) {
      // the caller is responsible for logging if the source is malformed
      return null;
    }
  }

  private static List<SourceWrapper> wrapMappedObject(
      JsonNode object, Set<String> mappedPolicyIds) {
    List<SourceWrapper> wrappers = new ArrayList<>();
    for (Map.Entry<String, JsonNode> field : object.properties()) {
      if (!mappedPolicyIds.contains(field.getKey())) {
        continue;
      }
      ObjectNode singlePolicy = MAPPER.createObjectNode();
      singlePolicy.set(field.getKey(), field.getValue());
      wrappers.add(new JsonSourceWrapper(singlePolicy));
    }
    return Collections.unmodifiableList(wrappers);
  }

  private static List<SourceWrapper> wrapMappedArray(JsonNode array, Set<String> mappedPolicyIds) {
    List<SourceWrapper> wrappers = new ArrayList<>();
    for (JsonNode element : array) {
      if (!isMappedSinglePolicyObject(element, mappedPolicyIds)) {
        continue;
      }
      wrappers.add(new JsonSourceWrapper(element));
    }
    return Collections.unmodifiableList(wrappers);
  }

  private static boolean isMappedSinglePolicyObject(JsonNode node, Set<String> mappedPolicyIds) {
    return node.isObject()
        && node.size() == 1
        && mappedPolicyIds.contains(node.fieldNames().next());
  }
}
