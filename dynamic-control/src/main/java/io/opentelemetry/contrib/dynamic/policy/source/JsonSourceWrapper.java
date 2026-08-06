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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Source wrapper for one policy parsed from JSON text that matches the {@link
 * SourceFormat#JSONKEYVALUE} shape: either one keyed policy entry or a full policy object with an
 * {@code id} field. Examples of the two supported JSON structures are:
 *
 * <pre>{@code
 * {
 *   "policy-id": my-value
 * }
 * }</pre>
 *
 * and
 *
 * <pre>{@code
 * {
 *   "id": "policy-id",
 *   "name": "Description of the policy",
 *   "trace": {
 *     "match": [{"field1": "match1", "field2": "match2"}],
 *     "keep": {
 *       "target-to-change": my-value
 *     }
 *   }
 * }
 * }</pre>
 */
public final class JsonSourceWrapper implements SourceWrapper {
  private static final Logger logger = Logger.getLogger(JsonSourceWrapper.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final List<String> TARGET_FIELD_NAMES =
      Collections.unmodifiableList(Arrays.asList("log", "metric", "profile", "trace"));
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
    if (!node.isObject()) {
      return null;
    }
    JsonNode id = node.get("id");
    if (id != null) {
      return isStructurallyValidFullPolicy(node) ? id.asText() : null;
    }
    if (node.size() != 1) {
      return null;
    }
    return node.fieldNames().next();
  }

  @Override
  @Nullable
  public SourceWrapper withPolicyType(String policyType) {
    Objects.requireNonNull(policyType, "policyType cannot be null");
    JsonNode node = asJsonNode();
    if (isFullPolicyObject()) {
      ObjectNode remappedNode = ((ObjectNode) node).deepCopy();
      remappedNode.put("id", policyType);
      return new JsonSourceWrapper(remappedNode);
    }
    JsonNode value = getPolicyValue();
    if (value == null) {
      return null;
    }
    ObjectNode remappedNode = MAPPER.createObjectNode();
    remappedNode.set(policyType, value);
    return new JsonSourceWrapper(remappedNode);
  }

  public JsonNode asJsonNode() {
    return source;
  }

  /**
   * Returns the policy-specific value to validate.
   *
   * <p>For a keyed entry this is its mapped value. For a full policy object this is the target's
   * {@code keep} value, after the common policy envelope has been validated.
   */
  @Nullable
  public JsonNode getPolicyValue() {
    JsonNode node = asJsonNode();
    if (!node.isObject()) {
      return null;
    }
    if (node.has("id")) {
      JsonNode target = getFullPolicyTarget(node);
      return target == null ? null : target.get("keep");
    }
    return node.size() == 1 ? node.elements().next() : null;
  }

  /** Returns whether this wrapper contains a structurally valid full policy object. */
  public boolean isFullPolicyObject() {
    return isStructurallyValidFullPolicy(asJsonNode());
  }

  /**
   * Parses JSON text into one wrapper per policy object.
   *
   * <p>Input must be a JSON object containing policy ID/value pairs, a full policy object with an
   * {@code id} field, or an array of single-policy objects. A full policy must have a non-empty
   * {@code id} and {@code name}, exactly one supported target, a non-empty target {@code match}
   * array, and a target {@code keep} value. Entries whose policy ID is not present in {@code
   * mappedPolicyIds}, or whose shape is invalid, are skipped while valid entries continue through
   * the pipeline. An empty JSON object or array yields an empty list.
   *
   * @return an empty list if the source contains no valid mapped policies; a non-empty list of
   *     wrappers for valid mapped policies; or {@code null} if the text is not valid JSON or its
   *     root is neither an object nor an array
   * @param mappedPolicyIds configured policy IDs accepted as JSON object keys or full policy IDs
   * @throws NullPointerException if source or mappedPolicyIds is null
   */
  @Nullable
  public static List<SourceWrapper> parse(String source, Set<String> mappedPolicyIds) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(mappedPolicyIds, "mappedPolicyIds cannot be null");
    try {
      JsonNode parsed = MAPPER.readTree(source);
      if (parsed.isObject()) {
        SourceWrapper wrapper = wrapMappedObject(parsed, mappedPolicyIds);
        if (wrapper == null) {
          logDroppedObject(parsed);
        }
        return wrapper == null ? Collections.emptyList() : Collections.singletonList(wrapper);
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

  /**
   * Returns {@code true} if {@code source} is valid JSON describing a single top-level object with
   * exactly one key.
   *
   * <p>Unlike {@link #parse(String, Set)}, this does not drop unmapped keys: it reports the shape
   * of the raw text. Callers that require exactly one policy per entry (for example, a file-backed
   * provider with one policy per line) use this to reject objects carrying extra keys rather than
   * silently discarding them.
   *
   * @throws NullPointerException if source is null
   */
  public static boolean isSingleKeyObject(String source) {
    Objects.requireNonNull(source, "source cannot be null");
    try {
      JsonNode parsed = MAPPER.readTree(source);
      return parsed.isObject() && parsed.size() == 1;
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  /**
   * Returns {@code true} if {@code source} is a single keyed policy object or a full policy object
   * with a non-empty textual {@code id}.
   *
   * @throws NullPointerException if source is null
   */
  public static boolean isSinglePolicyObject(String source) {
    Objects.requireNonNull(source, "source cannot be null");
    try {
      JsonNode parsed = MAPPER.readTree(source);
      if (!parsed.isObject()) {
        return false;
      }
      if (parsed.has("id")) {
        return isStructurallyValidFullPolicy(parsed);
      }
      return parsed.size() == 1;
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  @Nullable
  private static SourceWrapper wrapMappedObject(JsonNode object, Set<String> mappedPolicyIds) {
    if (object.has("id")) {
      if (isMappedFullPolicyObject(object, mappedPolicyIds)) {
        return new JsonSourceWrapper(object);
      }
      return null;
    }
    if (isMappedSinglePolicyObject(object, mappedPolicyIds)) {
      return new JsonSourceWrapper(object);
    }
    return null;
  }

  private static List<SourceWrapper> wrapMappedArray(JsonNode array, Set<String> mappedPolicyIds) {
    List<SourceWrapper> wrappers = new ArrayList<>();
    for (JsonNode element : array) {
      if (isMappedFullPolicyObject(element, mappedPolicyIds)
          || isMappedSinglePolicyObject(element, mappedPolicyIds)) {
        wrappers.add(new JsonSourceWrapper(element));
      } else if (element.isObject()) {
        logDroppedObject(element);
      }
    }
    return Collections.unmodifiableList(wrappers);
  }

  private static boolean isMappedSinglePolicyObject(JsonNode node, Set<String> mappedPolicyIds) {
    return node.isObject()
        && node.size() == 1
        && mappedPolicyIds.contains(node.fieldNames().next());
  }

  private static boolean isMappedFullPolicyObject(JsonNode node, Set<String> mappedPolicyIds) {
    JsonNode id = node.get("id");
    return id != null
        && mappedPolicyIds.contains(id.asText())
        && isStructurallyValidFullPolicy(node);
  }

  private static boolean isStructurallyValidFullPolicy(JsonNode node) {
    return node.isObject()
        && hasRequiredText(node, "id")
        && hasRequiredText(node, "name")
        && getFullPolicyTarget(node) != null;
  }

  @Nullable
  // The details of the structure are not yet fully specced, so this is just enforcing a minimal
  // shape
  private static JsonNode getFullPolicyTarget(JsonNode policyNode) {
    JsonNode target = null;
    for (String fieldName : TARGET_FIELD_NAMES) {
      JsonNode candidate = policyNode.get(fieldName);
      if (candidate == null) {
        continue;
      }
      // A full policy must contain exactly one target field.
      if (target != null) {
        return null;
      }
      target = candidate;
    }
    if (target == null || !target.isObject()) {
      return null;
    }
    JsonNode match = target.get("match");
    if (match == null || !match.isArray() || match.isEmpty()) {
      return null;
    }
    for (JsonNode matcher : match) {
      if (!matcher.isObject() || matcher.isEmpty()) {
        return null;
      }
    }
    JsonNode keep = target.get("keep");
    return keep == null || keep.isNull() ? null : target;
  }

  private static boolean hasRequiredText(JsonNode node, String fieldName) {
    return hasNonEmptyText(node.get(fieldName));
  }

  private static boolean hasNonEmptyText(@Nullable JsonNode value) {
    return value != null && value.isTextual() && !value.asText().trim().isEmpty();
  }

  private static void logDroppedObject(JsonNode object) {
    if (logger.isLoggable(java.util.logging.Level.FINE)) {
      logger.fine("Ignoring invalid or unmapped JSON policy object: " + object);
    }
  }
}
