/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class AzureAppServiceResourceProvider extends CloudResourceProvider {

  static final AttributeKey<String> AZURE_APP_SERVICE_STAMP_RESOURCE_ATTRIBUTE =
      AttributeKey.stringKey("azure.app.service.stamp");
  static final String REGION_NAME = "REGION_NAME";
  private static final String WEBSITE_HOME_STAMPNAME = "WEBSITE_HOME_STAMPNAME";
  private static final String WEBSITE_HOSTNAME = "WEBSITE_HOSTNAME";
  static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";
  private static final String WEBSITE_OWNER_NAME = "WEBSITE_OWNER_NAME";
  private static final String WEBSITE_RESOURCE_GROUP = "WEBSITE_RESOURCE_GROUP";
  static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
  private static final String WEBSITE_SLOT_NAME = "WEBSITE_SLOT_NAME";

  private static final Map<AttributeKey<String>, String> ENV_VAR_MAPPING = new HashMap<>();

  static {
    ENV_VAR_MAPPING.put(ResourceAttributes.CLOUD_REGION, REGION_NAME);
    ENV_VAR_MAPPING.put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, WEBSITE_SLOT_NAME);
    ENV_VAR_MAPPING.put(ResourceAttributes.HOST_ID, WEBSITE_HOSTNAME);
    ENV_VAR_MAPPING.put(ResourceAttributes.SERVICE_INSTANCE_ID, WEBSITE_INSTANCE_ID);
    ENV_VAR_MAPPING.put(AZURE_APP_SERVICE_STAMP_RESOURCE_ATTRIBUTE, WEBSITE_HOME_STAMPNAME);
  }

  private final Map<String, String> env;

  // SPI
  public AzureAppServiceResourceProvider() {
    this(System.getenv());
  }

  // Visible for testing
  AzureAppServiceResourceProvider(Map<String, String> env) {
    this.env = env;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    AzureEnvVarPlatform detect = AzureEnvVarPlatform.detect(env);
    if (detect != AzureEnvVarPlatform.APP_SERVICE) {
      return Resource.empty();
    }
    String name = Objects.requireNonNull(env.get(WEBSITE_SITE_NAME));
    AttributesBuilder builder = AzureVmResourceProvider.azureAttributeBuilder();
    builder.put(
        ResourceAttributes.CLOUD_PLATFORM,
        ResourceAttributes.CloudPlatformValues.AZURE_APP_SERVICE);
    builder.put(ResourceAttributes.SERVICE_NAME, name);

    String resourceUri = resourceUri(name);
    if (resourceUri != null) {
      builder.put(ResourceAttributes.CLOUD_RESOURCE_ID, resourceUri);
      AzureEnvVarPlatform.addAttributesFromEnv(ENV_VAR_MAPPING, env, builder);
    }

    return Resource.create(builder.build());
  }

  @Nullable
  private String resourceUri(String websiteName) {
    String websiteResourceGroup = env.get(WEBSITE_RESOURCE_GROUP);
    String websiteOwnerName = env.get(WEBSITE_OWNER_NAME);

    String subscriptionId;
    if (websiteOwnerName != null && websiteOwnerName.contains("+")) {
      subscriptionId = websiteOwnerName.substring(0, websiteOwnerName.indexOf("+"));
    } else {
      subscriptionId = websiteOwnerName;
    }

    if (StringUtils.isNullOrEmpty(websiteResourceGroup)
        || StringUtils.isNullOrEmpty(subscriptionId)) {
      return null;
    }

    return String.format(
        "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s",
        subscriptionId, websiteResourceGroup, websiteName);
  }
}
