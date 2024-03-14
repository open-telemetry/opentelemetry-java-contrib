/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Map;

public enum AzureEnvVarPlatform {
  APP_SERVICE,
  FUNCTIONS,
  NONE;

  static AzureEnvVarPlatform detect(Map<String, String> env) {
    String name = env.get(AzureAppServiceResourceProvider.WEBSITE_SITE_NAME);
    if (name == null) {
      return NONE;
    }
    if (env.get(AzureFunctionsResourceProvider.FUNCTIONS_VERSION) != null) {
      return FUNCTIONS;
    }
    return APP_SERVICE;
  }

  static void addAttributesFromEnv(
      Map<AttributeKey<String>, String> mapping,
      Map<String, String> env,
      AttributesBuilder builder) {
    mapping.forEach(
        (key, value) -> {
          String envValue = env.get(value);
          if (envValue != null) {
            builder.put(key, envValue);
          }
        });
  }
}
