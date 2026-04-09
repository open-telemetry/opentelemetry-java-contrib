/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolicyInitConfigTest {

  @TempDir Path tempDir;

  @Test
  void readFromConfigPropertiesReturnsNullWhenBothPathsMissing() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(null);

    assertThat(PolicyInitConfig.readFromConfigProperties(config)).isNull();
  }

  @Test
  void readFromConfigPropertiesPrefersYamlWhenBothPathsPresent() throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-json").getBytes(StandardCharsets.UTF_8));
    Path yamlPath = tempDir.resolve("policy-init.yaml");
    Files.write(yamlPath, yamlWithLocation("from-yaml").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML))
        .thenReturn(yamlPath.toString());
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    PolicyInitConfig initConfig = PolicyInitConfig.readFromConfigProperties(config);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-yaml");
  }

  @Test
  void readFromConfigPropertiesFallsBackToJsonWhenYamlBlank() throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-json").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn("  ");
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    PolicyInitConfig initConfig = PolicyInitConfig.readFromConfigProperties(config);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-json");
  }

  @Test
  void readFromConfigPropertiesReadsJsonWhenYamlPathMissing() throws Exception {
    Path configPath = tempDir.resolve("policy-init.json");
    Files.write(configPath, minimalJsonConfig().getBytes(StandardCharsets.UTF_8));

    PolicyInitConfig config = configFromPaths(null, configPath.toString());

    assertThat(config.getSources()).hasSize(1);
    PolicySourceConfig source = config.getSources().get(0);
    assertThat(source.getKind()).isEqualTo(SourceKind.OPAMP);
    assertThat(source.getFormat()).isEqualTo(SourceFormat.JSONKEYVALUE);
    assertThat(source.getLocation()).isEqualTo("vendor");
    assertThat(source.getMappings()).hasSize(1);
    assertThat(source.getMappings().get(0).getSourceKey()).isEqualTo("sampling_rate");
    assertThat(source.getMappings().get(0).getPolicyType()).isEqualTo("trace_sampling_rate_policy");
  }

  @Test
  void readFromConfigPropertiesReadsYamlWhenYamlPathProvided() throws Exception {
    Path configPath = tempDir.resolve("policy-init.yaml");
    Files.write(configPath, minimalYamlConfig().getBytes(StandardCharsets.UTF_8));

    PolicyInitConfig config = configFromPaths(configPath.toString(), null);

    assertThat(config.getSources()).hasSize(1);
    PolicySourceConfig source = config.getSources().get(0);
    assertThat(source.getKind()).isEqualTo(SourceKind.OPAMP);
    assertThat(source.getFormat()).isEqualTo(SourceFormat.JSONKEYVALUE);
    assertThat(source.getMappings()).hasSize(1);
  }

  @Test
  void readFromConfigPropertiesReturnsNullOnFileReadFailure() {
    Path missing = tempDir.resolve("missing-policy-init.json");

    assertThat(configFromPaths(null, missing.toString())).isNull();
  }

  @Test
  void readFromConfigPropertiesReturnsNullOnParseFailure() throws Exception {
    Path badJson = tempDir.resolve("bad-policy-init.json");
    Files.write(badJson, "{}".getBytes(StandardCharsets.UTF_8));

    assertThat(configFromPaths(null, badJson.toString())).isNull();
  }

  private static PolicyInitConfig configFromPaths(String yamlPath, String jsonPath) {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(yamlPath);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(jsonPath);
    return PolicyInitConfig.readFromConfigProperties(config);
  }

  private static String minimalJsonConfig() {
    return "{\"sources\":[{\"kind\":\"opamp\",\"format\":\"jsonkeyvalue\",\"location\":\"vendor\","
        + "\"mappings\":[{\"sourceKey\":\"sampling_rate\",\"policyType\":\"trace_sampling_rate_policy\"}]}]}";
  }

  private static String jsonWithLocation(String location) {
    return "{\"sources\":[{\"kind\":\"opamp\",\"format\":\"jsonkeyvalue\",\"location\":\""
        + location
        + "\",\"mappings\":[{\"sourceKey\":\"sampling_rate\",\"policyType\":\"trace_sampling_rate_policy\"}]}]}";
  }

  private static String minimalYamlConfig() {
    return "sources:\n"
        + "  - kind: opamp\n"
        + "    format: jsonkeyvalue\n"
        + "    location: vendor\n"
        + "    mappings:\n"
        + "      - sourceKey: sampling_rate\n"
        + "        policyType: trace_sampling_rate_policy\n";
  }

  private static String yamlWithLocation(String location) {
    return "sources:\n"
        + "  - kind: opamp\n"
        + "    format: jsonkeyvalue\n"
        + "    location: "
        + location
        + "\n"
        + "    mappings:\n"
        + "      - sourceKey: sampling_rate\n"
        + "        policyType: trace_sampling_rate_policy\n";
  }
}
