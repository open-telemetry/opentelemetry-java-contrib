/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInitConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceConfig;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class PolicyInitConfigReaderTest {

  private static final String EXAMPLE_FIXTURE =
      "/io/opentelemetry/contrib/dynamic/policy/registry/json/policy-init-example.json";

  private final PolicyInitConfigReader reader = new PolicyInitConfigReader();

  @Test
  void readsSourceCentricFixture() throws Exception {
    try (InputStream in = getClass().getResourceAsStream(EXAMPLE_FIXTURE)) {
      assertThat(in).isNotNull();

      PolicyInitConfig config = reader.read(in);
      assertThat(config.getSources()).hasSize(1);

      PolicySourceConfig source = config.getSources().get(0);
      assertThat(source.getKind()).isEqualTo(SourceKind.OPAMP);
      assertThat(source.getFormat()).isEqualTo(SourceFormat.JSONKEYVALUE);
      assertThat(source.getLocation()).isEqualTo("vendor-specific");
      assertThat(source.getMappings()).hasSize(4);
      assertThat(source.getMappings().get(0).getSourceKey()).isEqualTo("sampling_rate");
      assertThat(source.getMappings().get(0).getPolicyType())
          .isEqualTo("trace_sampling_rate_policy");
    }
  }

  @Test
  void missingSourcesThrows() {
    assertThatThrownBy(() -> reader.read("{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sources");
  }

  @Test
  void emptyPayloadThrows() {
    assertThatThrownBy(() -> reader.read(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("(?s).*(empty|sources).*");
  }

  @Test
  void missingFormatThrows() {
    String json = "{\"sources\":[{\"kind\":\"opamp\",\"mappings\":[]}]}";
    assertThatThrownBy(() -> reader.read(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("format");
  }

  @Test
  void missingMappingsThrows() {
    String json = "{\"sources\":[{\"kind\":\"opamp\",\"format\":\"jsonkeyvalue\"}]}";
    assertThatThrownBy(() -> reader.read(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mappings");
  }

  @Test
  void mappingMissingSourceKeyThrows() {
    String json =
        "{\"sources\":[{\"kind\":\"opamp\",\"format\":\"jsonkeyvalue\",\"mappings\":[{\"policyType\":\"x\"}]}]}";
    assertThatThrownBy(() -> reader.read(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sourceKey");
  }
}
