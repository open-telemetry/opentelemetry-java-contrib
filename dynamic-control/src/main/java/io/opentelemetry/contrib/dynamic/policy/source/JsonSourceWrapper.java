/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Source wrapper for policy payloads parsed from JSON text that matches the {@link
 * SourceFormat#JSONKEYVALUE} shape: each policy is a JSON object with exactly one top-level key
 * (the policy type) and one value (the payload). The on-the-wire syntax is standard JSON, not a
 * separate encoding.
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
   * <p>Input must be valid JSON whose structure matches {@link SourceFormat#JSONKEYVALUE}: either a
   * single JSON object with exactly one top-level key/value pair, or a JSON array of such objects.
   * An empty JSON array {@code []} yields an empty list.
   *
   * @return an empty list if the source is an empty JSON array {@code []}; a non-empty list of
   *     wrappers if the source is a valid single-policy object or non-empty array of such objects;
   *     or {@code null} if the text is not valid JSON or the value shape is not supported for
   *     {@link SourceFormat#JSONKEYVALUE}.
   * @throws NullPointerException if source is null
   */
  @Nullable
  public static List<SourceWrapper> parse(String source) {
    Objects.requireNonNull(source, "source cannot be null");
    try {
      JsonNode parsed = MAPPER.readTree(source);
      if (!isSupportedJsonShape(parsed)) {
        return null;
      }
      if (parsed.isObject()) {
        return Collections.singletonList(new JsonSourceWrapper(parsed));
      }
      List<SourceWrapper> wrappers = new ArrayList<>();
      for (JsonNode element : parsed) {
        wrappers.add(new JsonSourceWrapper(element));
      }
      return Collections.unmodifiableList(wrappers);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private static boolean isSupportedJsonShape(JsonNode node) {
    if (node.isObject()) {
      return isSinglePolicyObject(node);
    }
    if (!node.isArray()) {
      return false;
    }
    for (JsonNode element : node) {
      if (!isSinglePolicyObject(element)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isSinglePolicyObject(JsonNode node) {
    return node.isObject() && node.size() == 1;
  }
}
