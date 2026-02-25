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

/** JSON-backed source wrapper for a single-policy object. */
public final class JsonSourceWrapper implements SourceWrapper {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final JsonNode source;

  public JsonSourceWrapper(JsonNode source) {
    this.source = Objects.requireNonNull(source, "source cannot be null");
  }

  @Override
  public SourceFormat getFormat() {
    return SourceFormat.JSON;
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
   * Parses JSON source into one wrapper per top-level policy object.
   *
   * @return an empty list if the source is an empty JSON array; a non-empty list of wrappers if the
   *     source is a valid single-policy object or array thereof; or {@code null} if the shape is
   *     unsupported or the source is not valid JSON.
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
