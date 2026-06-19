/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpampPolicyProviderTest {

  @Test
  void getEndpointReturnsNullWhenUnset() {
    ConfigProperties properties = mock(ConfigProperties.class);
    when(properties.getString("otel.opamp.service.url")).thenReturn(null);

    assertThat(OpampPolicyProvider.getEndpoint(properties)).isNull();
  }

  @Test
  void getEndpointAppendsOpampPathWhenMissing() {
    ConfigProperties properties = mock(ConfigProperties.class);
    when(properties.getString("otel.opamp.service.url")).thenReturn("https://example.com/base");

    assertThat(OpampPolicyProvider.getEndpoint(properties))
        .isEqualTo("https://example.com/base/v1/opamp");
  }

  @Test
  void getServiceNameUsesPrimaryPropertyFirst() {
    ConfigProperties properties = mock(ConfigProperties.class);
    when(properties.getString("otel.service.name")).thenReturn("my-service");
    when(properties.getMap("otel.resource.attributes")).thenReturn(Collections.emptyMap());

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("my-service");
  }

  @Test
  void getServiceNameFallsBackToResourceAttribute() {
    ConfigProperties properties = mock(ConfigProperties.class);
    Map<String, String> resourceAttributes = new HashMap<>();
    resourceAttributes.put("service.name", "resource-service");
    when(properties.getString("otel.service.name")).thenReturn(null);
    when(properties.getMap("otel.resource.attributes")).thenReturn(resourceAttributes);

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("resource-service");
  }

  @Test
  void getServiceEnvironmentUsesSemconvThenLegacy() {
    ConfigProperties semconvProperties = mock(ConfigProperties.class);
    Map<String, String> semconvResourceAttributes = new HashMap<>();
    semconvResourceAttributes.put("deployment.environment.name", "prod");
    semconvResourceAttributes.put("deployment.environment", "legacy");
    when(semconvProperties.getMap("otel.resource.attributes"))
        .thenReturn(semconvResourceAttributes);
    assertThat(OpampPolicyProvider.getServiceEnvironment(semconvProperties)).isEqualTo("prod");

    ConfigProperties legacyProperties = mock(ConfigProperties.class);
    Map<String, String> legacyResourceAttributes = new HashMap<>();
    legacyResourceAttributes.put("deployment.environment", "staging");
    when(legacyProperties.getMap("otel.resource.attributes")).thenReturn(legacyResourceAttributes);
    assertThat(OpampPolicyProvider.getServiceEnvironment(legacyProperties)).isEqualTo("staging");
  }

  @Test
  void resetForTestRestoresThirtySecondDefaultPollingInterval() {
    OpampPolicyProvider.setGlobalPollingInterval(Duration.ofSeconds(5));
    OpampPolicyProvider.resetForTest();

    assertThat(OpampPolicyProvider.getGlobalPollingIntervalForTest())
        .isEqualTo(Duration.ofSeconds(30));
  }
}
