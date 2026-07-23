/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PolicyProviderConfigTest {

  @Test
  void copiesAndProtectsLegacyMaps() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    Map<String, String> resourceAttributes = new HashMap<>();
    resourceAttributes.put("service.name", "legacy-service");
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer token");

    PolicyProviderConfig config =
        PolicyProviderConfig.createWithLegacyProperties(properties, resourceAttributes, headers);
    resourceAttributes.put("deployment.environment", "prod");
    headers.put("X-Changed", "after-construction");

    assertThat(config.getProperties()).isSameAs(properties);
    assertThat(config.getResourceAttributes()).containsEntry("service.name", "legacy-service");
    assertThat(config.getResourceAttributes()).doesNotContainKey("deployment.environment");
    assertThat(config.getOpampHeaders()).containsEntry("Authorization", "Bearer token");
    assertThat(config.getOpampHeaders()).doesNotContainKey("X-Changed");
    assertThatThrownBy(() -> config.getOpampHeaders().put("X-New", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void defaultsToEmptyLegacyMaps() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);

    assertThat(PolicyProviderConfig.create(properties).getResourceAttributes()).isEmpty();
    assertThat(PolicyProviderConfig.create(properties).getOpampHeaders()).isEmpty();
  }
}
