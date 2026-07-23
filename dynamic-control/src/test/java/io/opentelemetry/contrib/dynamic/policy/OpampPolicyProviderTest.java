/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import okio.Buffer;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.Test;

class OpampPolicyProviderTest {

  @StartStop private final MockWebServer server = new MockWebServer();

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

    assertThat(OpampPolicyProvider.getServiceName(properties)).isEqualTo("my-service");
  }

  @Test
  void getServiceNameFallsBackToResourceAttribute() {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties resourceAttributes = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.service.name")).thenReturn(null);
    when(properties.get("otel.resource.attributes")).thenReturn(resourceAttributes);
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
    when(semconvResourceAttributes.getString("deployment.environment.name")).thenReturn("prod");
    when(semconvResourceAttributes.getString("deployment.environment")).thenReturn("legacy");
    assertThat(OpampPolicyProvider.getServiceEnvironment(semconvProperties)).isEqualTo("prod");

    DeclarativeConfigProperties legacyProperties = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties legacyResourceAttributes = mock(DeclarativeConfigProperties.class);
    when(legacyProperties.get("otel.resource.attributes")).thenReturn(legacyResourceAttributes);
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

  @Test
  void startWatchingAddsConfiguredHeadersToOpampRequests() throws Exception {
    DeclarativeConfigProperties properties = mock(DeclarativeConfigProperties.class);
    when(properties.getString("otel.opamp.service.url"))
        .thenReturn(server.url("/v1/opamp").toString());
    when(properties.getString("otel.service.name")).thenReturn("test-service");
    when(properties.get("otel.resource.attributes"))
        .thenReturn(DeclarativeConfigProperties.empty());

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer token");
    headers.put("X-Test-Header", "test-value");
    OpampPolicyProvider provider =
        new OpampPolicyProvider(
            new PolicyProviderConfig(properties, headers),
            "vendor-specific",
            SourceFormat.KEYVALUE,
            Collections.emptyList(),
            Collections.emptyList());
    server.enqueue(emptyServerResponse());

    try (Closeable ignored = provider.startWatching(policies -> {})) {
      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);

      assertThat(request).isNotNull();
      assertThat(request.getHeaders().get("Authorization")).isEqualTo("Bearer token");
      assertThat(request.getHeaders().get("X-Test-Header")).isEqualTo("test-value");
    } finally {
      OpampPolicyProvider.resetForTest();
    }
  }

  private static MockResponse emptyServerResponse() {
    Buffer body = new Buffer();
    body.write(new ServerToAgent.Builder().build().encode());
    return new MockResponse.Builder().code(200).body(body).build();
  }
}
