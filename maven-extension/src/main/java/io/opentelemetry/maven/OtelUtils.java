/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import com.google.common.collect.Lists;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class OtelUtils {
  protected static String prettyPrintSdkConfiguration(
      AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    List<String> configAttributeNames =
        Lists.newArrayList(
            "otel.traces.exporter",
            "otel.metrics.exporter",
            "otel.exporter.otlp.endpoint",
            "otel.exporter.otlp.traces.endpoint",
            "otel.exporter.otlp.metrics.endpoint",
            "otel.exporter.jaeger.endpoint",
            "otel.exporter.prometheus.port",
            "otel.resource.attributes",
            "otel.service.name");

    ConfigProperties sdkConfig = autoConfiguredOpenTelemetrySdk.getConfig();
    Map<String, String> configurationAttributes = new LinkedHashMap<>();
    for (String attributeName : configAttributeNames) {
      final String attributeValue = sdkConfig.getString(attributeName);
      if (attributeValue != null) {
        configurationAttributes.put(attributeName, attributeValue);
      }
    }

    Resource sdkResource = autoConfiguredOpenTelemetrySdk.getResource();

    return "Configuration: "
        + configurationAttributes.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", "))
        + ", Resource: "
        + sdkResource.getAttributes();
  }

  public static Optional<String> getSysPropOrEnvVar(String systemPropertyName) {
    String systemPropertyValue = System.getProperty(systemPropertyName);
    if (systemPropertyValue != null) {
      return Optional.of(systemPropertyValue);
    }
    String environmentVariableName = systemPropertyName.replace('.', '_').toUpperCase(Locale.ROOT);
    String environmentVariable = System.getenv().get(environmentVariableName);
    if (environmentVariable != null) {
      return Optional.of(environmentVariable);
    }
    return Optional.empty();
  }

  public static Optional<Boolean> getBooleanSysPropOrEnvVar(String systemPropertyName) {
    Optional<String> valueAsString = getSysPropOrEnvVar(systemPropertyName);
    if (valueAsString.isPresent()) {
      return Optional.of(Boolean.parseBoolean(valueAsString.get()));
    } else {
      return Optional.empty();
    }
  }
}
