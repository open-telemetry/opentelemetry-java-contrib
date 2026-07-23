/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigBridge;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyConfigPropertiesBridgeTest {

  @Test
  void exposesGeneralScalarPropertiesThroughTheDeclarativeBridge() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString("otel.opamp.service.url")).thenReturn("https://example.com");

    DeclarativeConfigProperties properties =
        LegacyConfigPropertiesBridge.create(config, componentProperties(config));

    assertThat(properties.getString("otel.opamp.service.url")).isEqualTo("https://example.com");
  }

  @Test
  void exposesLegacyMapsAsStructuredProperties() {
    ConfigProperties config = mock(ConfigProperties.class);
    Map<String, String> resourceAttributes = new HashMap<>();
    resourceAttributes.put("service.name", "legacy-service");
    resourceAttributes.put("deployment.environment", "prod");
    when(config.getMap("otel.resource.attributes")).thenReturn(resourceAttributes);
    Map<String, String> headers = Collections.singletonMap("Authorization", "Bearer token");
    when(config.getMap("otel.experimental.opamp.headers")).thenReturn(headers);

    DeclarativeConfigProperties properties =
        LegacyConfigPropertiesBridge.create(config, componentProperties(config));

    assertThat(properties.get("otel.resource.attributes").getString("service.name"))
        .isEqualTo("legacy-service");
    assertThat(properties.get("otel.resource.attributes").getPropertyKeys())
        .containsExactlyInAnyOrder("service.name", "deployment.environment");
    assertThat(LegacyConfigPropertiesBridge.getOpampHeaders(config))
        .containsEntry("Authorization", "Bearer token");
  }

  private static DeclarativeConfigProperties componentProperties(ConfigProperties config) {
    return DeclarativeConfigBridge.createComponentProperties(config, "");
  }
}
