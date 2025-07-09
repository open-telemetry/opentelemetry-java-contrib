/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AZURE_APP_SERVICE;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.HOST_ID;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.sdk.resources.Resource;

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
    ENV_VAR_MAPPING.put(CLOUD_REGION, REGION_NAME);
    ENV_VAR_MAPPING.put(DEPLOYMENT_ENVIRONMENT_NAME, WEBSITE_SLOT_NAME);
    ENV_VAR_MAPPING.put(HOST_ID, WEBSITE_HOSTNAME);
    ENV_VAR_MAPPING.put(SERVICE_INSTANCE_ID, WEBSITE_INSTANCE_ID);
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
  public Resource createResource() {
    return Resource.create(getAttributes());
  }

  public Attributes getAttributes() {
    AzureEnvVarPlatform detect = AzureEnvVarPlatform.detect(env);
    if (detect != AzureEnvVarPlatform.APP_SERVICE) {
      return Attributes.empty();
    }
    String name = Objects.requireNonNull(env.get(WEBSITE_SITE_NAME));
    AttributesBuilder builder = AzureVmResourceProvider.azureAttributeBuilder(AZURE_APP_SERVICE);
    builder.put(SERVICE_NAME, name);

    String resourceUri = resourceUri(name);
    if (resourceUri != null) {
      builder.put(CLOUD_RESOURCE_ID, resourceUri);
    }

    AzureEnvVarPlatform.addAttributesFromEnv(ENV_VAR_MAPPING, env, builder);

    return builder.build();
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
