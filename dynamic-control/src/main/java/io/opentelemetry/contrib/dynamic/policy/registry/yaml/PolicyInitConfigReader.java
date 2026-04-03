/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInitConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.json.JsonNodePolicyInitConfigParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Reads {@link PolicyInitConfig} from YAML (registry initialization file or payload). */
public final class PolicyInitConfigReader {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  public PolicyInitConfig read(InputStream in) throws IOException {
    Objects.requireNonNull(in, "in cannot be null");
    JsonNode root = MAPPER.readTree(in);
    return JsonNodePolicyInitConfigParser.parse(root);
  }

  public PolicyInitConfig read(String yaml) throws IOException {
    Objects.requireNonNull(yaml, "yaml cannot be null");
    return read(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }
}
