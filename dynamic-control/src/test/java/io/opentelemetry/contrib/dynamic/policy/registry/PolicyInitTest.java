/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PolicyInitTest {

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
  void doesNotInitializePolicyWhenTopLevelTelemetryPolicyDeclarativeConfigMissing() {
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
}
