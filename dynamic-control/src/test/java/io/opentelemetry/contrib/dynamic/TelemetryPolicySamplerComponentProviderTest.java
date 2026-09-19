/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import okio.Buffer;
import opamp.proto.AgentToServer;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

class TelemetryPolicySamplerComponentProviderTest {
  @StartStop private final MockWebServer server = new MockWebServer();

  @AfterEach
  void tearDown() throws Exception {
    invokeStaticNoArg(PolicyInit.class, "resetForTest");
    invokeStaticNoArg(TraceSamplingRatePolicy.class, "resetForTest");
  }

  @Test
  @ClearSystemProperty(key = "otel.opamp.service.url")
  // A legacy fallback must not accidentally satisfy the detected-name assertion.
  @SetSystemProperty(key = "otel.service.name", value = "legacy-service")
  @SetSystemProperty(
      key = "otel.resource.attributes",
      value = "service.name=legacy-resource-service")
  @SetSystemProperty(key = "otel.experimental.opamp.headers", value = "")
  void declarativeConfigCallbackStartsOpampWithDetectedServiceName() throws Exception {
    String yaml =
        "file_format: '1.0'\n"
            + "resource:\n"
            + "  detection/development:\n"
            + "    detectors:\n"
            + "      - test_service: {}\n"
            + "tracer_provider:\n"
            + "  sampler:\n"
            + "    telemetry_policy/development:\n"
            + "      sources:\n"
            + "        - kind: opamp\n"
            + "          format: jsonkeyvalue\n"
            + "          location: vendor\n"
            + "          mappings:\n"
            + "            - policyId: sampling-rate\n"
            + "              policyType: "
            + TraceSamplingRatePolicy.POLICY_TYPE
            + "\n";
    System.setProperty("otel.opamp.service.url", server.url("/v1/opamp").toString());
    server.enqueue(
        new MockResponse.Builder()
            .body(new Buffer().write(new ServerToAgent.Builder().build().encode()))
            .build());
    // No custom component loader or manual callback: both the sampler and test detector must
    // be discovered through SPI, and source activation must wait for the built SDK resource.
    try (OpenTelemetrySdk sdk =
        DeclarativeConfiguration.parseAndCreate(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
            .getSdk()) {
      assertThat(sdk.getSdkTracerProvider().getSampler())
          .isSameAs(TraceSamplingRatePolicy.getInitializedSampler());
      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertThat(request).isNotNull();
      assertThat(request.getBody()).isNotNull();
      AgentToServer message = AgentToServer.ADAPTER.decode(request.getBody());
      assertThat(message.agent_description).isNotNull();
      assertThat(message.agent_description.identifying_attributes)
          .anySatisfy(
              attribute -> {
                assertThat(attribute.key).isEqualTo("service.name");
                assertThat(attribute.value.string_value).isEqualTo("detected-service");
              });
    }
  }

  @Test
  void initializesPolicyFromTopLevelTelemetryPolicyDeclarativeConfig() {
    TelemetryPolicySamplerComponentProvider provider =
        new TelemetryPolicySamplerComponentProvider();
    provider.create(telemetryPolicyNodeConfig());

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
  }

  @Test
  void doesNothingWhenTelemetryPolicyDeclarativeConfigMissing() {
    TelemetryPolicySamplerComponentProvider provider =
        new TelemetryPolicySamplerComponentProvider();
    provider.create(mock(DeclarativeConfigProperties.class));

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNull();
  }

  private static DeclarativeConfigProperties telemetryPolicyNodeConfig() {
    DeclarativeConfigProperties telemetryPolicy = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties source = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties mapping = mock(DeclarativeConfigProperties.class);

    when(telemetryPolicy.getStructuredList("sources"))
        .thenReturn(Collections.singletonList(source));
    when(source.getString("kind")).thenReturn("opamp");
    when(source.getString("format")).thenReturn("jsonkeyvalue");
    when(source.getString("location")).thenReturn("vendor");
    when(source.getStructuredList("mappings")).thenReturn(Collections.singletonList(mapping));
    when(mapping.getString("policyId")).thenReturn("sampling-rate");
    when(mapping.getString("policyType")).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);

    return telemetryPolicy;
  }

  private static void invokeStaticNoArg(Class<?> targetClass, String methodName) throws Exception {
    Method method = targetClass.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(null);
  }
}
