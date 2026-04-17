/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.ExtendedOpenTelemetry;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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

  @Test
  void readFromDeclarativeConfigPropertiesReturnsNullWhenTelemetryPolicyMissing() {
    DeclarativeConfigProperties root = mock(DeclarativeConfigProperties.class);
    when(root.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY)).thenReturn(null);

    assertThat(PolicyInitConfig.readFromDeclarativeConfigProperties(root)).isNull();
  }

  @Test
  void readFromDeclarativeConfigPropertiesReadsTelemetryPolicySources() {
    DeclarativeConfigProperties root = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties telemetryPolicy = telemetryPolicyConfig("from-declarative");
    when(root.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(telemetryPolicy);

    PolicyInitConfig config = PolicyInitConfig.readFromDeclarativeConfigProperties(root);

    assertThat(config).isNotNull();
    assertThat(config.getSources()).hasSize(1);
    PolicySourceConfig source = config.getSources().get(0);
    assertThat(source.getKind()).isEqualTo(SourceKind.OPAMP);
    assertThat(source.getFormat()).isEqualTo(SourceFormat.JSONKEYVALUE);
    assertThat(source.getLocation()).isEqualTo("from-declarative");
    assertThat(source.getMappings()).hasSize(1);
    assertThat(source.getMappings().get(0).getSourceKey()).isEqualTo("sampling_rate");
    assertThat(source.getMappings().get(0).getPolicyType()).isEqualTo("trace_sampling_rate_policy");
  }

  @Test
  void readFromDeclarativeOrConfigPropertiesPrefersDeclarativeWhenPresent() throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    DeclarativeConfigProperties declarativeRoot = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties telemetryPolicy = telemetryPolicyConfig("from-declarative");
    when(declarativeRoot.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(telemetryPolicy);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromDeclarativeOrConfigProperties(config, declarativeRoot);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-declarative");
  }

  @Test
  void readFromDeclarativeOrConfigPropertiesFallsBackToFileWhenTelemetryPolicyMissing()
      throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    DeclarativeConfigProperties declarativeRoot = mock(DeclarativeConfigProperties.class);
    when(declarativeRoot.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(null);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromDeclarativeOrConfigProperties(config, declarativeRoot);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-file");
  }

  @Test
  void readFromOpenTelemetryOrConfigPropertiesPrefersDeclarativeWhenExtendedOpenTelemetry()
      throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    ExtendedOpenTelemetry openTelemetry = mock(ExtendedOpenTelemetry.class);
    ConfigProvider configProvider = mock(ConfigProvider.class);
    DeclarativeConfigProperties declarativeRoot = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties telemetryPolicy = telemetryPolicyConfig("from-declarative");
    when(openTelemetry.getConfigProvider()).thenReturn(configProvider);
    when(configProvider.getGeneralInstrumentationConfig()).thenReturn(declarativeRoot);
    when(declarativeRoot.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(telemetryPolicy);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromOpenTelemetryOrConfigProperties(config, openTelemetry);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-declarative");
  }

  @Test
  void readFromOpenTelemetryOrConfigPropertiesUsesGeneralConfigAccessorWhenAvailable()
      throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    ExtendedOpenTelemetry openTelemetry = mock(ExtendedOpenTelemetry.class);
    ConfigProvider configProvider =
        new ConfigProviderWithGeneralConfig(telemetryPolicyRootConfig("from-general-config"));
    when(openTelemetry.getConfigProvider()).thenReturn(configProvider);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromOpenTelemetryOrConfigProperties(config, openTelemetry);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-general-config");
  }

  @Test
  void readFromOpenTelemetryOrConfigPropertiesFallsBackWhenNotExtendedOpenTelemetry()
      throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    OpenTelemetry openTelemetry = mock(OpenTelemetry.class);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromOpenTelemetryOrConfigProperties(config, openTelemetry);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-file");
  }

  @Test
  void readFromOpenTelemetryOrConfigPropertiesFallsBackWhenTelemetryPolicyMissingInGeneralConfig()
      throws Exception {
    Path jsonPath = tempDir.resolve("policy-init.json");
    Files.write(jsonPath, jsonWithLocation("from-file").getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(jsonPath.toString());

    ExtendedOpenTelemetry openTelemetry = mock(ExtendedOpenTelemetry.class);
    ConfigProvider configProvider = mock(ConfigProvider.class);
    DeclarativeConfigProperties generalConfig = mock(DeclarativeConfigProperties.class);
    when(openTelemetry.getConfigProvider()).thenReturn(configProvider);
    when(configProvider.getGeneralInstrumentationConfig()).thenReturn(generalConfig);
    when(generalConfig.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(null);

    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromOpenTelemetryOrConfigProperties(config, openTelemetry);

    assertThat(initConfig).isNotNull();
    assertThat(initConfig.getSources()).hasSize(1);
    assertThat(initConfig.getSources().get(0).getLocation()).isEqualTo("from-file");
  }

  private static PolicyInitConfig configFromPaths(String yamlPath, String jsonPath) {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(yamlPath);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(jsonPath);
    return PolicyInitConfig.readFromConfigProperties(config);
  }

  private static DeclarativeConfigProperties telemetryPolicyConfig(String location) {
    DeclarativeConfigProperties telemetryPolicy = mock(DeclarativeConfigProperties.class);
    List<DeclarativeConfigProperties> sources = Collections.singletonList(sourceConfig(location));
    when(telemetryPolicy.getStructuredList(PolicyInitConfig.SOURCES_DECLARATIVE_KEY))
        .thenReturn(sources);
    return telemetryPolicy;
  }

  private static DeclarativeConfigProperties telemetryPolicyRootConfig(String location) {
    DeclarativeConfigProperties root = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties telemetryPolicy = telemetryPolicyConfig(location);
    when(root.getStructured(PolicyInitConfig.TELEMETRY_POLICY_DECLARATIVE_KEY))
        .thenReturn(telemetryPolicy);
    return root;
  }

  private static DeclarativeConfigProperties sourceConfig(String location) {
    DeclarativeConfigProperties source = mock(DeclarativeConfigProperties.class);
    when(source.getString(PolicyInitConfig.KIND_DECLARATIVE_KEY)).thenReturn("opamp");
    when(source.getString(PolicyInitConfig.FORMAT_DECLARATIVE_KEY)).thenReturn("jsonkeyvalue");
    when(source.getString(PolicyInitConfig.LOCATION_DECLARATIVE_KEY)).thenReturn(location);
    List<DeclarativeConfigProperties> mappings = Collections.singletonList(mappingConfig());
    when(source.getStructuredList(PolicyInitConfig.MAPPINGS_DECLARATIVE_KEY)).thenReturn(mappings);
    return source;
  }

  private static DeclarativeConfigProperties mappingConfig() {
    DeclarativeConfigProperties mapping = mock(DeclarativeConfigProperties.class);
    when(mapping.getString(PolicyInitConfig.SOURCE_KEY_DECLARATIVE_KEY))
        .thenReturn("sampling_rate");
    when(mapping.getString(PolicyInitConfig.POLICY_TYPE_DECLARATIVE_KEY))
        .thenReturn("trace_sampling_rate_policy");
    return mapping;
  }

  @SuppressWarnings("EffectivelyPrivate")
  private static final class ConfigProviderWithGeneralConfig implements ConfigProvider {
    private final DeclarativeConfigProperties generalConfig;

    private ConfigProviderWithGeneralConfig(DeclarativeConfigProperties generalConfig) {
      this.generalConfig = generalConfig;
    }

    @Override
    public DeclarativeConfigProperties getInstrumentationConfig() {
      return DeclarativeConfigProperties.empty();
    }

    @SuppressWarnings("unused")
    public DeclarativeConfigProperties getGeneralConfig() {
      return generalConfig;
    }
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
