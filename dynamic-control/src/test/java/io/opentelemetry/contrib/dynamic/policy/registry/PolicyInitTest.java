/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicyIdentity;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class PolicyInitTest {
  @TempDir Path tempDir;

  @AfterEach
  void tearDown() throws Exception {
    PolicyInit.resetForTest();
    invokeStaticNoArg(TraceSamplingRatePolicy.class, "resetForTest");
  }

  @Test
  void doesNotInitializePolicyFromDeclarativeOnlyConfigInAutoConfigurationMode() {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);
    PolicyInit.init(customizer);
    Function<ConfigProperties, Map<String, String>> propertiesCustomizer =
        capturePropertiesCustomizer(customizer);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(null);
    Map<String, String> ignored = propertiesCustomizer.apply(config);

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNull();
    assertThat(ignored).isNotNull();
  }

  @Test
  void initializesPolicyFromInitConfigInAutoConfigurationMode() throws Exception {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);
    PolicyInit.init(customizer);
    Function<ConfigProperties, Map<String, String>> propertiesCustomizer =
        capturePropertiesCustomizer(customizer);

    Path configPath = tempDir.resolve("policy-init.json");
    Files.write(configPath, minimalJsonInitConfig().getBytes(StandardCharsets.UTF_8));

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(PolicyInitConfig.POLICY_INIT_CONFIG_PROPERTY_JSON))
        .thenReturn(configPath.toString());

    Map<String, String> ignored = propertiesCustomizer.apply(config);

    assertThat(ignored).isNotNull();
    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
  }

  @Test
  void initializesRegisteredPolicyTypeFromDeclarativeConfig() {
    ConfigProperties config = mock(ConfigProperties.class);

    PolicyInit.initFromDeclarativeConfig(
        telemetryPolicyNodeConfig(TraceSamplingRatePolicy.POLICY_TYPE), config);

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
  }

  @Test
  void throwsWhenDeclarativeConfigUsesUnknownPolicyType() {
    ConfigProperties config = mock(ConfigProperties.class);

    assertThatThrownBy(
            () ->
                PolicyInit.initFromDeclarativeConfig(
                    telemetryPolicyNodeConfig("trace_sampling_rate_policy"), config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown policyType");
  }

  @Test
  void initializesPolicyClassOnlyOnceAcrossRepeatedInitCalls() {
    String policyType = "test-policy-idempotent";
    AtomicInteger initializeCount = new AtomicInteger();
    PolicyImplementer implementer = mock(PolicyImplementer.class);
    when(implementer.getValidators()).thenReturn(Collections.emptyList());
    PolicyInit.registerPolicyType(
        policyType,
        IdempotentTestPolicy.class,
        autoConfiguration -> {
          initializeCount.incrementAndGet();
          return implementer;
        });
    ConfigProperties config = mock(ConfigProperties.class);

    PolicyInit.initFromDeclarativeConfig(telemetryPolicyNodeConfig(policyType), config);
    PolicyInit.initFromDeclarativeConfig(telemetryPolicyNodeConfig(policyType), config);

    assertThat(initializeCount.get()).isEqualTo(1);
    verify(implementer, times(1)).onPoliciesChanged(Collections.emptyList());
  }

  private static Function<ConfigProperties, Map<String, String>> capturePropertiesCustomizer(
      AutoConfigurationCustomizer customizer) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> captor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(captor.capture());
    return captor.getValue();
  }

  private static void invokeStaticNoArg(Class<?> targetClass, String methodName) throws Exception {
    Method method = targetClass.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(null);
  }

  private static DeclarativeConfigProperties telemetryPolicyNodeConfig(String policyType) {
    DeclarativeConfigProperties telemetryPolicy = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties source = mock(DeclarativeConfigProperties.class);
    DeclarativeConfigProperties mapping = mock(DeclarativeConfigProperties.class);

    when(telemetryPolicy.getStructuredList(PolicyInitConfig.SOURCES_DECLARATIVE_KEY))
        .thenReturn(Collections.singletonList(source));
    when(source.getString(PolicyInitConfig.KIND_DECLARATIVE_KEY)).thenReturn("opamp");
    when(source.getString(PolicyInitConfig.FORMAT_DECLARATIVE_KEY)).thenReturn("jsonkeyvalue");
    when(source.getString(PolicyInitConfig.LOCATION_DECLARATIVE_KEY)).thenReturn("vendor");
    when(source.getStructuredList(PolicyInitConfig.MAPPINGS_DECLARATIVE_KEY))
        .thenReturn(Collections.singletonList(mapping));
    when(mapping.getString(PolicyInitConfig.POLICY_ID_DECLARATIVE_KEY)).thenReturn("sampling_rate");
    when(mapping.getString(PolicyInitConfig.POLICY_TYPE_DECLARATIVE_KEY)).thenReturn(policyType);
    return telemetryPolicy;
  }

  private static String minimalJsonInitConfig() {
    return "{\"sources\":[{\"kind\":\"opamp\",\"format\":\"jsonkeyvalue\",\"location\":\"vendor\","
        + "\"mappings\":[{\"policyId\":\"sampling_rate\",\"policyType\":\""
        + TraceSamplingRatePolicy.POLICY_TYPE
        + "\"}]}]}";
  }

  private static final class IdempotentTestPolicy implements TelemetryPolicy {
    private static final TelemetryPolicyIdentity IDENTITY =
        new TelemetryPolicyIdentity("test-policy-idempotent", "Test policy idempotent");
    private final SourceKind sourceKind;

    private IdempotentTestPolicy() {
      this(SourceKind.CUSTOM);
    }

    private IdempotentTestPolicy(SourceKind sourceKind) {
      this.sourceKind = sourceKind;
    }

    @Override
    public TelemetryPolicyIdentity getIdentity() {
      return IDENTITY;
    }

    @Override
    public String getType() {
      return "test-policy-idempotent";
    }

    @Override
    public SourceKind getSourceKind() {
      return sourceKind;
    }
  }
}
