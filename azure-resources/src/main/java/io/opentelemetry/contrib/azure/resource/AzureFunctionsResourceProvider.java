/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;

public class AzureFunctionsResourceProvider extends CloudResourceProvider {

  static final String FUNCTIONS_VERSION = "FUNCTIONS_EXTENSION_VERSION";
  private static final String FUNCTIONS_MEM_LIMIT = "WEBSITE_MEMORY_LIMIT_MB";

  private static final Map<AttributeKey<String>, String> ENV_VAR_MAPPING = new HashMap<>();

  static {
    ENV_VAR_MAPPING.put(
        ResourceAttributes.CLOUD_REGION, AzureAppServiceResourceProvider.REGION_NAME);
    ENV_VAR_MAPPING.put(
        ResourceAttributes.FAAS_NAME, AzureAppServiceResourceProvider.WEBSITE_SITE_NAME);
    ENV_VAR_MAPPING.put(ResourceAttributes.FAAS_VERSION, FUNCTIONS_VERSION);
    ENV_VAR_MAPPING.put(
        ResourceAttributes.FAAS_INSTANCE, AzureAppServiceResourceProvider.WEBSITE_INSTANCE_ID);
  }

  private final Map<String, String> env;

  // SPI
  public AzureFunctionsResourceProvider() {
    this(System.getenv());
  }

  // Visible for testing
  AzureFunctionsResourceProvider(Map<String, String> env) {
    this.env = env;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    AzureEnvVarPlatform detect = AzureEnvVarPlatform.detect(env);
    if (detect != AzureEnvVarPlatform.FUNCTIONS) {
      return Resource.empty();
    }

    AttributesBuilder builder =
        AzureVmResourceProvider.azureAttributeBuilder(
            ResourceAttributes.CloudPlatformValues.AZURE_FUNCTIONS);

    String limit = env.get(FUNCTIONS_MEM_LIMIT);
    if (limit != null) {
      builder.put(ResourceAttributes.FAAS_MAX_MEMORY, Long.parseLong(limit));
    }

    AzureEnvVarPlatform.addAttributesFromEnv(ENV_VAR_MAPPING, env, builder);

    return Resource.create(builder.build());
  }
}
