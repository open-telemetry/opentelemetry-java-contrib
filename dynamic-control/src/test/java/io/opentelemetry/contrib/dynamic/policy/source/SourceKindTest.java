/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.PolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceConfig;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceKindTest {

  @Test
  void configValuesAreStableLowercase() {
    assertThat(SourceKind.FILE.configValue()).isEqualTo("file");
    assertThat(SourceKind.OPAMP.configValue()).isEqualTo("opamp");
    assertThat(SourceKind.HTTP.configValue()).isEqualTo("http");
    assertThat(SourceKind.CUSTOM.configValue()).isEqualTo("custom");
  }

  @Test
  void fromConfigValueParsesCaseInsensitive() {
    assertThat(SourceKind.fromConfigValue("FILE")).isEqualTo(SourceKind.FILE);
    assertThat(SourceKind.fromConfigValue("Opamp")).isEqualTo(SourceKind.OPAMP);
  }

  @Test
  void fromConfigValueTrimsWhitespace() {
    assertThat(SourceKind.fromConfigValue("  http  ")).isEqualTo(SourceKind.HTTP);
  }

  @Test
  void fromConfigValueRejectsNullInput() {
    assertThatThrownBy(() -> SourceKind.fromConfigValue(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
  }

  @Test
  void fromConfigValueRejectsUnknownValue() {
    assertThatThrownBy(() -> SourceKind.fromConfigValue("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown source kind: unknown");
  }

  @Test
  void createProviderReturnsNullForKindsWithoutProviderCreator() {
    PolicySourceConfig source = source(SourceKind.FILE, "ignored");
    ConfigProperties config = opampConfig();
    List<PolicyValidator> validators = Collections.emptyList();

    assertThat(SourceKind.FILE.createProvider(source, config, validators)).isNull();
    assertThat(
            SourceKind.HTTP.createProvider(source(SourceKind.HTTP, "ignored"), config, validators))
        .isNull();
    assertThat(
            SourceKind.CUSTOM.createProvider(
                source(SourceKind.CUSTOM, "ignored"), config, validators))
        .isNull();
  }

  @Test
  void opampCreateProviderReturnsNullWhenLocationMissing() {
    ConfigProperties config = opampConfig();
    List<PolicyValidator> validators = Collections.emptyList();

    assertThat(SourceKind.OPAMP.createProvider(source(SourceKind.OPAMP, null), config, validators))
        .isNull();
    assertThat(SourceKind.OPAMP.createProvider(source(SourceKind.OPAMP, "  "), config, validators))
        .isNull();
  }

  @Test
  void opampCreateProviderReturnsProviderWhenLocationPresent() {
    PolicyProvider provider =
        SourceKind.OPAMP.createProvider(
            source(SourceKind.OPAMP, "vendor-specific"), opampConfig(), Collections.emptyList());

    assertThat(provider).isNotNull();
  }

  @Test
  void opampCreateProviderReturnsNullWhenRequiredConfigMissing() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("otel.opamp.service.url")).thenReturn(null);
    when(config.getString("otel.service.name")).thenReturn("test-service");
    when(config.getMap("otel.experimental.opamp.headers")).thenReturn(Collections.emptyMap());
    when(config.getMap("otel.resource.attributes")).thenReturn(Collections.emptyMap());

    PolicyProvider provider =
        SourceKind.OPAMP.createProvider(
            source(SourceKind.OPAMP, "vendor-specific"), config, Collections.emptyList());

    assertThat(provider).isNull();
  }

  @Test
  void createProviderRejectsNullArguments() {
    ConfigProperties config = opampConfig();
    PolicySourceConfig source = source(SourceKind.OPAMP, "vendor-specific");
    List<PolicyValidator> validators = Collections.emptyList();

    assertThatThrownBy(() -> SourceKind.OPAMP.createProvider(null, config, validators))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
    assertThatThrownBy(() -> SourceKind.OPAMP.createProvider(source, null, validators))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("config cannot be null");
    assertThatThrownBy(() -> SourceKind.OPAMP.createProvider(source, config, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("validators cannot be null");
  }

  private static PolicySourceConfig source(SourceKind kind, String location) {
    return new PolicySourceConfig(kind, SourceFormat.KEYVALUE, location, Collections.emptyList());
  }

  private static ConfigProperties opampConfig() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("otel.opamp.service.url")).thenReturn("https://example.com");
    when(config.getString("otel.service.name")).thenReturn("test-service");
    when(config.getMap("otel.experimental.opamp.headers")).thenReturn(Collections.emptyMap());
    when(config.getMap("otel.resource.attributes")).thenReturn(Collections.emptyMap());
    return config;
  }
}
