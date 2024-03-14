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
import javax.annotation.Nullable;

public class AzureAppServiceResourceProvider extends CloudResourceProvider {

  static final AttributeKey<String> AZURE_APP_SERVICE_STAMP_RESOURCE_ATTRIBUTE =
      AttributeKey.stringKey("azure.app.service.stamp");
  private static final String REGION_NAME = "REGION_NAME";
  private static final String WEBSITE_HOME_STAMPNAME = "WEBSITE_HOME_STAMPNAME";
  private static final String WEBSITE_HOSTNAME = "WEBSITE_HOSTNAME";
  private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";
  private static final String WEBSITE_OWNER_NAME = "WEBSITE_OWNER_NAME";
  private static final String WEBSITE_RESOURCE_GROUP = "WEBSITE_RESOURCE_GROUP";
  private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
  private static final String WEBSITE_SLOT_NAME = "WEBSITE_SLOT_NAME";

  private static final Map<AttributeKey<String>, String> APP_SERVICE_ATTRIBUTE_ENV_VARS =
      new HashMap<>();

  static {
    APP_SERVICE_ATTRIBUTE_ENV_VARS.put(ResourceAttributes.CLOUD_REGION, REGION_NAME);
    APP_SERVICE_ATTRIBUTE_ENV_VARS.put(
        ResourceAttributes.DEPLOYMENT_ENVIRONMENT, WEBSITE_SLOT_NAME);
    APP_SERVICE_ATTRIBUTE_ENV_VARS.put(ResourceAttributes.HOST_ID, WEBSITE_HOSTNAME);
    APP_SERVICE_ATTRIBUTE_ENV_VARS.put(ResourceAttributes.SERVICE_INSTANCE_ID, WEBSITE_INSTANCE_ID);
    APP_SERVICE_ATTRIBUTE_ENV_VARS.put(
        AZURE_APP_SERVICE_STAMP_RESOURCE_ATTRIBUTE, WEBSITE_HOME_STAMPNAME);
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
    String name = env.get(WEBSITE_SITE_NAME);
    if (name == null) {
      return Resource.empty();
    }
    AttributesBuilder builder = AzureVmResourceProvider.azureAttributeBuilder();
    builder.put(ResourceAttributes.CLOUD_PLATFORM, "azure_app_service");
    builder.put(ResourceAttributes.SERVICE_NAME, name);

    String resourceUri = resourceUri(name);
    if (resourceUri != null) {
      builder.put(ResourceAttributes.CLOUD_RESOURCE_ID, resourceUri);
      APP_SERVICE_ATTRIBUTE_ENV_VARS.forEach(
          (key, value) -> {
            String envValue = env.get(value);
            if (envValue != null) {
              builder.put(key, envValue);
            }
          });
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
