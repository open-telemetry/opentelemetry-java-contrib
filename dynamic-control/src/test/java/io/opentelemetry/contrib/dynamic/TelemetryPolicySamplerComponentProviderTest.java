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
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TelemetryPolicySamplerComponentProviderTest {

  @AfterEach
  void tearDown() throws Exception {
    invokeStaticNoArg(PolicyInit.class, "resetForTest");
    invokeStaticNoArg(TraceSamplingRatePolicy.class, "resetForTest");
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
