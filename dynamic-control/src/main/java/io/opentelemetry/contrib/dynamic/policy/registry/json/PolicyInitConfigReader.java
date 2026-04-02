/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInitConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Reads {@link PolicyInitConfig} from JSON (registry initialization file or payload). */
public final class PolicyInitConfigReader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public PolicyInitConfig read(String json) throws IOException {
    Objects.requireNonNull(json, "json cannot be null");
    return read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
  }

  public PolicyInitConfig read(InputStream in) throws IOException {
    Objects.requireNonNull(in, "in cannot be null");
    JsonNode root = MAPPER.readTree(in);
    return JsonNodePolicyInitConfigParser.parse(root);
  }
}
