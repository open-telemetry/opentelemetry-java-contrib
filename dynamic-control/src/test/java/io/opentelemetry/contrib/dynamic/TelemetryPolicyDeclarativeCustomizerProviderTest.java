/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.AbstractTraceSamplingPolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeNameValueModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TelemetryPolicyDeclarativeCustomizerProviderTest {
  private static final String POLICY_INIT_CONFIG_PROPERTY_JSON =
      "otel.java.experimental.telemetry.policy.init.json";
  private static final String POLICY_INIT_CONFIG_PROPERTY_YAML =
      "otel.java.experimental.telemetry.policy.init.yaml";

  @AfterEach
  void tearDown() throws Exception {
    invokeStaticNoArg(PolicyInit.class, "resetForTest");
    invokeStaticNoArg(TraceSamplingRatePolicy.class, "resetForTest");
  }

  @Test
  void initializesTopLevelTelemetryPolicyWithoutSamplerComponentDeclaration() {
    Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel> modelCustomizer =
        captureModelCustomizer();

    OpenTelemetryConfigurationModel customized =
        modelCustomizer.apply(topLevelTelemetryPolicyModel(TraceSamplingRatePolicy.POLICY_TYPE));
    assertThat(customized).isNotNull();
    Object samplerProperty =
        customized
            .getTracerProvider()
            .getSampler()
            .getExtensionProperties()
            .get(TelemetryPolicySamplerComponentProvider.NAME);
    assertThat(samplerProperty).isNotNull();
    assertThat(samplerProperty).isInstanceOf(Map.class);
    Map<?, ?> samplerPropertyMap = (Map<?, ?>) samplerProperty;
    assertThat(samplerPropertyMap.get("sources")).isNotNull();
    assertThat(samplerPropertyMap.get("resource_attributes")).isEqualTo(resourceAttributes());
    assertThat(samplerPropertyMap.get("otel.resource.attributes")).isEqualTo(resourceAttributes());
    assertThat(customized.getResource().getAttributes())
        .extracting(AttributeNameValueModel::getName)
        .contains("service.name");

    AutoConfigurationCustomizer autoConfiguration = mock(AutoConfigurationCustomizer.class);
    PolicyInit.init(autoConfiguration);
    Function<ConfigProperties, Map<String, String>> propertiesCustomizer =
        capturePropertiesCustomizer(autoConfiguration);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(POLICY_INIT_CONFIG_PROPERTY_YAML)).thenReturn(null);
    when(config.getString(POLICY_INIT_CONFIG_PROPERTY_JSON)).thenReturn(null);

    assertThat(propertiesCustomizer.apply(config)).isNotNull();
    assertThat(AbstractTraceSamplingPolicy.getInitializedSampler()).isNotNull();
  }

  @Test
  void invalidTopLevelTelemetryPolicyThrows() {
    Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel> modelCustomizer =
        captureModelCustomizer();

    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    model.setExtensionProperty(
        TelemetryPolicyDeclarativeCustomizerProvider.TELEMETRY_POLICY_TOP_LEVEL_KEY,
        Collections.singletonMap("sources", "not-an-array"));

    assertThatThrownBy(() -> modelCustomizer.apply(model))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sources");
  }

  private static Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>
      captureModelCustomizer() {
    DeclarativeConfigurationCustomizer customizer = mock(DeclarativeConfigurationCustomizer.class);
    new TelemetryPolicyDeclarativeCustomizerProvider().customize(customizer);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>>
        captor = ArgumentCaptor.forClass(Function.class);
    verify(customizer).addModelCustomizer(captor.capture());
    return captor.getValue();
  }

  private static Function<ConfigProperties, Map<String, String>> capturePropertiesCustomizer(
      AutoConfigurationCustomizer customizer) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> captor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(captor.capture());
    return captor.getValue();
  }

  private static OpenTelemetryConfigurationModel topLevelTelemetryPolicyModel(String policyType) {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    model.setExtensionProperty(
        TelemetryPolicyDeclarativeCustomizerProvider.TELEMETRY_POLICY_TOP_LEVEL_KEY,
        telemetryPolicy(policyType));
    model.setResource(
        new ResourceModel()
            .setAttributes(
                Arrays.asList(
                    new AttributeNameValueModel().setName("service.name").setValue("edot-otel"),
                    new AttributeNameValueModel()
                        .setName("deployment.environment.name")
                        .setValue("dev"))));
    return model;
  }

  private static Map<String, Object> telemetryPolicy(String policyType) {
    Map<String, Object> mapping = new LinkedHashMap<>();
    mapping.put("policyId", "sampling-rate");
    mapping.put("policyType", policyType);

    Map<String, Object> source = new LinkedHashMap<>();
    source.put("kind", "opamp");
    source.put("format", "jsonkeyvalue");
    source.put("location", "elastic");
    source.put("mappings", Collections.singletonList(mapping));

    Map<String, Object> telemetryPolicy = new LinkedHashMap<>();
    telemetryPolicy.put("sources", Collections.singletonList(source));
    return telemetryPolicy;
  }

  private static Map<String, String> resourceAttributes() {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("service.name", "edot-otel");
    attributes.put("deployment.environment.name", "dev");
    return attributes;
  }

  private static void invokeStaticNoArg(Class<?> targetClass, String methodName) throws Exception {
    Method method = targetClass.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(null);
  }
}
