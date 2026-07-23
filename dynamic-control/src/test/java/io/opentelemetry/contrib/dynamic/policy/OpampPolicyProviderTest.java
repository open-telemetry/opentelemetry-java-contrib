/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class OpampPolicyProviderTest {

  @Test
  void getEndpointReturnsNullWhenUnset() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.opamp.service.url")).thenReturn(null);

    assertThat(OpampPolicyProvider.getEndpoint(properties)).isNull();
  }

  @Test
  void getEndpointAppendsOpampPathWhenMissing() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.opamp.service.url")).thenReturn("https://example.com/base");

    assertThat(OpampPolicyProvider.getEndpoint(properties))
        .isEqualTo("https://example.com/base/v1/opamp");
  }

  @Test
  void getServiceNameUsesPrimaryPropertyFirst() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties resourceAttributes = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.service.name")).thenReturn("my-service");
    when(properties.get("otel.resource.attributes")).thenReturn(resourceAttributes);
    when(resourceAttributes.getPropertyKeys()).thenReturn(Collections.emptySet());

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("my-service");
  }

  @Test
  void getServiceNameFallsBackToResourceAttribute() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties resourceAttributes = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.service.name")).thenReturn(null);
    when(properties.get("otel.resource.attributes")).thenReturn(resourceAttributes);
    when(resourceAttributes.getPropertyKeys()).thenReturn(Collections.singleton("service.name"));
    when(resourceAttributes.getString("service.name")).thenReturn("resource-service");

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("resource-service");
  }

  @Test
  void getServiceNameUsesUnknownServiceWhenResourceAttributesAreUnset() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.service.name")).thenReturn(null);
    when(properties.get("otel.resource.attributes"))
        .thenReturn(DeclarativeConfigProperties.empty());

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("unknown_service:java");
  }

  @Test
  void getServiceEnvironmentUsesSemconvThenLegacy() {
    DeclarativeConfigProperties semconvProperties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties semconvResourceAttributes = mock(DeclarativeConfigProperties.class);
    when(semconvProperties.get("otel.resource.attributes")).thenReturn(semconvResourceAttributes);
    when(semconvResourceAttributes.getPropertyKeys())
        .thenReturn(
            new HashSet<>(Arrays.asList("deployment.environment.name", "deployment.environment")));
    when(semconvResourceAttributes.getString("deployment.environment.name")).thenReturn("prod");
    when(semconvResourceAttributes.getString("deployment.environment")).thenReturn("legacy");
    assertThat(OpampPolicyProvider.getServiceEnvironment(semconvProperties)).isEqualTo("prod");

    DeclarativeConfigProperties legacyProperties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties legacyResourceAttributes = mock(DeclarativeConfigProperties.class);
    when(legacyProperties.get("otel.resource.attributes")).thenReturn(legacyResourceAttributes);
    when(legacyResourceAttributes.getPropertyKeys())
        .thenReturn(Collections.singleton("deployment.environment"));
    when(legacyResourceAttributes.getString("deployment.environment")).thenReturn("staging");
    assertThat(OpampPolicyProvider.getServiceEnvironment(legacyProperties)).isEqualTo("staging");
  }

  @Test
  void getServiceEnvironmentReturnsNullWhenResourceAttributesAreUnset() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    when(properties.get("otel.resource.attributes"))
        .thenReturn(DeclarativeConfigProperties.empty());

    assertThat(OpampPolicyProvider.getServiceEnvironment(properties)).isNull();
  }

  @Test
  void resetForTestRestoresThirtySecondDefaultPollingInterval() {
    OpampPolicyProvider.setGlobalPollingInterval(Duration.ofSeconds(5));
    OpampPolicyProvider.resetForTest();

    assertThat(OpampPolicyProvider.getGlobalPollingIntervalForTest())
        .isEqualTo(Duration.ofSeconds(30));
  }
}
