/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AZURE_FUNCTIONS;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.FAAS_INSTANCE;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.FAAS_MAX_MEMORY;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.FAAS_NAME;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.FAAS_VERSION;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.HashMap;
import java.util.Map;

public class AzureFunctionsResourceProvider extends CloudResourceProvider {

  static final String FUNCTIONS_VERSION = "FUNCTIONS_EXTENSION_VERSION";
  private static final String FUNCTIONS_MEM_LIMIT = "WEBSITE_MEMORY_LIMIT_MB";

  private static final Map<AttributeKey<String>, String> ENV_VAR_MAPPING = new HashMap<>();

  static {
    ENV_VAR_MAPPING.put(CLOUD_REGION, AzureAppServiceResourceProvider.REGION_NAME);
    ENV_VAR_MAPPING.put(FAAS_NAME, AzureAppServiceResourceProvider.WEBSITE_SITE_NAME);
    ENV_VAR_MAPPING.put(FAAS_VERSION, FUNCTIONS_VERSION);
    ENV_VAR_MAPPING.put(FAAS_INSTANCE, AzureAppServiceResourceProvider.WEBSITE_INSTANCE_ID);
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
  public Resource createResource() {
    return Resource.create(getAttributes());
  }

  public Attributes getAttributes() {
    AzureEnvVarPlatform detect = AzureEnvVarPlatform.detect(env);
    if (detect != AzureEnvVarPlatform.FUNCTIONS) {
      return Attributes.empty();
    }

    AttributesBuilder builder = AzureVmResourceProvider.azureAttributeBuilder(AZURE_FUNCTIONS);

    String limit = env.get(FUNCTIONS_MEM_LIMIT);
    if (limit != null) {
      builder.put(FAAS_MAX_MEMORY, Long.parseLong(limit));
    }

    AzureEnvVarPlatform.addAttributesFromEnv(ENV_VAR_MAPPING, env, builder);

    return builder.build();
  }
}
