/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInitConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.registry.json.JsonNodePolicyInitConfigParser;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeNameValueModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Declarative config customizer for top-level telemetry policy configuration.
 *
 * <p>The Java agent version currently used by this module exposes the incubating fileconfig
 * customizer SPI. This is the declarative-config hook corresponding to the newer agent {@code
 * sdk.autoconfigure.declarativeconfig} API.
 */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public final class TelemetryPolicyDeclarativeCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  static final String TELEMETRY_POLICY_TOP_LEVEL_KEY = "telemetry_policy/development";

  private static final Logger logger =
      Logger.getLogger(TelemetryPolicyDeclarativeCustomizerProvider.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          registerTopLevelTelemetryPolicy(model);
          return model;
        });
  }

  private static void registerTopLevelTelemetryPolicy(OpenTelemetryConfigurationModel model) {
    if (model == null || model.getAdditionalProperties() == null) {
      return;
    }
    Map<String, Object> additionalProperties = model.getAdditionalProperties();
    Object telemetryPolicy = additionalProperties.remove(TELEMETRY_POLICY_TOP_LEVEL_KEY);
    if (telemetryPolicy == null) {
      return;
    }
    if (!(telemetryPolicy instanceof Map)) {
      logger.log(
          Level.WARNING,
          "Ignoring top-level declarative telemetry policy config because it is not an object");
      return;
    }

    Map<?, ?> telemetryPolicyConfig = (Map<?, ?>) telemetryPolicy;
    JsonNode telemetryPolicyNode = MAPPER.valueToTree(telemetryPolicyConfig);
    PolicyInitConfig initConfig = JsonNodePolicyInitConfigParser.parse(telemetryPolicyNode);
    PolicyInit.setDeclarativeInitConfig(initConfig);
    maybeInstallTelemetryPolicySampler(model, telemetryPolicyConfig, initConfig);
    logger.log(
        Level.INFO,
        "Dynamic control extension has loaded top-level declarative telemetry policy config with {0} source(s)",
        initConfig.getSources().size());
  }

  private static void maybeInstallTelemetryPolicySampler(
      OpenTelemetryConfigurationModel model,
      Map<?, ?> telemetryPolicy,
      PolicyInitConfig initConfig) {
    if (!containsPolicyType(initConfig, TraceSamplingRatePolicy.POLICY_TYPE)) {
      return;
    }
    Object sources = telemetryPolicy.get("sources");
    if (sources == null) {
      return;
    }
    TracerProviderModel tracerProvider = model.getTracerProvider();
    if (tracerProvider == null) {
      tracerProvider = new TracerProviderModel();
      model.withTracerProvider(tracerProvider);
    }
    SamplerModel sampler = tracerProvider.getSampler();
    if (sampler == null) {
      sampler = new SamplerModel();
      tracerProvider.withSampler(sampler);
    }
    sampler.withAdditionalProperty(
        TelemetryPolicySamplerComponentProvider.NAME,
        new SamplerPropertyModel()
            .withAdditionalProperty("sources", sources)
            .withAdditionalProperty("resource_attributes", resourceAttributes(model))
            .withAdditionalProperty("otel.resource.attributes", resourceAttributes(model)));
  }

  private static boolean containsPolicyType(PolicyInitConfig initConfig, String policyType) {
    for (PolicySourceConfig source : initConfig.getSources()) {
      for (PolicySourceMappingConfig mapping : source.getMappings()) {
        if (policyType.equals(mapping.getPolicyType())) {
          return true;
        }
      }
    }
    return false;
  }

  private static Map<String, String> resourceAttributes(OpenTelemetryConfigurationModel model) {
    ResourceModel resource = model.getResource();
    if (resource == null || resource.getAttributes() == null) {
      return Collections.emptyMap();
    }
    Map<String, String> attributes = new LinkedHashMap<>();
    List<AttributeNameValueModel> resourceAttributes = resource.getAttributes();
    for (AttributeNameValueModel attribute : resourceAttributes) {
      if (attribute.getName() != null && attribute.getValue() != null) {
        attributes.put(attribute.getName(), String.valueOf(attribute.getValue()));
      }
    }
    return Collections.unmodifiableMap(attributes);
  }
}
