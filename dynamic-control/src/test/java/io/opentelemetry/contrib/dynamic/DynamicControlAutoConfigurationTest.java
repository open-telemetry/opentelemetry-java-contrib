/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class DynamicControlAutoConfigurationTest {
  private static final String POLICY_INIT_CONFIG_PROPERTY_JSON =
      "otel.java.experimental.telemetry.policy.init.json";
  private static final String POLICY_INIT_CONFIG_PROPERTY_YAML =
      "otel.java.experimental.telemetry.policy.init.yaml";

  @TempDir Path tempDir;

  @AfterEach
  void tearDown() throws Exception {
    invokeStaticNoArg(PolicyInit.class, "resetForTest");
    invokeStaticNoArg(TraceSamplingRatePolicy.class, "resetForTest");
  }

  @Test
  void customizeRegistersPropertiesCustomizerThatInitializesConfiguredPolicy() throws Exception {
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    config.customize(customizer);
    Function<ConfigProperties, Map<String, String>> propertiesCustomizer =
        capturePropertiesCustomizer(customizer);

    Path configPath = tempDir.resolve("policy-init.yaml");
    Files.write(configPath, minimalYamlInitConfig().getBytes(StandardCharsets.UTF_8));
    ConfigProperties properties = mock(ConfigProperties.class);
    when(properties.getString(POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(configPath.toString());
    when(properties.getString(POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(null);

    Map<String, String> overrides = propertiesCustomizer.apply(properties);

    assertThat(overrides).isEmpty();
    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
  }

  @Test
  void testOrder() {
    // This is a placeholder test, just to have something
    DynamicControlAutoConfiguration config = new DynamicControlAutoConfiguration();
    // Default order should be 0
    assertThat(config.order()).isEqualTo(0);
  }

  private static Function<ConfigProperties, Map<String, String>> capturePropertiesCustomizer(
      AutoConfigurationCustomizer customizer) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> captor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(captor.capture());
    return captor.getValue();
  }

  private static String minimalYamlInitConfig() {
    return "sources:\n"
        + "  - kind: opamp\n"
        + "    format: jsonkeyvalue\n"
        + "    location: vendor\n"
        + "    mappings:\n"
        + "      - policyId: sampling_rate\n"
        + "        policyType: "
        + TraceSamplingRatePolicy.POLICY_TYPE
        + "\n";
  }

  private static void invokeStaticNoArg(Class<?> targetClass, String methodName) throws Exception {
    Method method = targetClass.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(null);
  }
}
