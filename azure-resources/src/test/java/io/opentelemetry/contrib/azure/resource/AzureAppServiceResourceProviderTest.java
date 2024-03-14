/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class AzureAppServiceResourceProviderTest {

  private static final String TEST_WEBSITE_SITE_NAME = "TEST_WEBSITE_SITE_NAME";
  private static final String TEST_REGION_NAME = "TEST_REGION_NAME";
  private static final String TEST_WEBSITE_SLOT_NAME = "TEST_WEBSITE_SLOT_NAME";
  private static final String TEST_WEBSITE_HOSTNAME = "TEST_WEBSITE_HOSTNAME";
  private static final String TEST_WEBSITE_INSTANCE_ID = "TEST_WEBSITE_INSTANCE_ID";
  private static final String TEST_WEBSITE_HOME_STAMPNAME = "TEST_WEBSITE_HOME_STAMPNAME";
  private static final String TEST_WEBSITE_RESOURCE_GROUP = "TEST_WEBSITE_RESOURCE_GROUP";
  private static final String TEST_WEBSITE_OWNER_NAME = "TEST_WEBSITE_OWNER_NAME";
  private static final ImmutableMap<String, String> DEFAULT_ENV_VARS =
      ImmutableMap.of(
          "WEBSITE_SITE_NAME", TEST_WEBSITE_SITE_NAME,
          "REGION_NAME", TEST_REGION_NAME,
          "WEBSITE_SLOT_NAME", TEST_WEBSITE_SLOT_NAME,
          "WEBSITE_HOSTNAME", TEST_WEBSITE_HOSTNAME,
          "WEBSITE_INSTANCE_ID", TEST_WEBSITE_INSTANCE_ID,
          "WEBSITE_HOME_STAMPNAME", TEST_WEBSITE_HOME_STAMPNAME,
          "WEBSITE_RESOURCE_GROUP", TEST_WEBSITE_RESOURCE_GROUP,
          "WEBSITE_OWNER_NAME", TEST_WEBSITE_OWNER_NAME);

  @Test
  void defaultValues() {
    createResource(DEFAULT_ENV_VARS)
        .containsEntry(ResourceAttributes.SERVICE_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "azure")
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "azure_app_service")
        .containsEntry(
            ResourceAttributes.CLOUD_RESOURCE_ID,
            "/subscriptions/TEST_WEBSITE_OWNER_NAME/resourceGroups/TEST_WEBSITE_RESOURCE_GROUP/providers/Microsoft.Web/sites/TEST_WEBSITE_SITE_NAME")
        .containsEntry(ResourceAttributes.CLOUD_REGION, TEST_REGION_NAME)
        .containsEntry(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, TEST_WEBSITE_SLOT_NAME)
        .containsEntry(ResourceAttributes.HOST_ID, TEST_WEBSITE_HOSTNAME)
        .containsEntry(ResourceAttributes.SERVICE_INSTANCE_ID, TEST_WEBSITE_INSTANCE_ID)
        .containsEntry(
            AzureAppServiceResourceProvider.AZURE_APP_SERVICE_STAMP_RESOURCE_ATTRIBUTE,
            TEST_WEBSITE_HOME_STAMPNAME);
  }

  @Test
  void subscriptionFromOwner() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.put("WEBSITE_OWNER_NAME", "foo+bar");

    createResource(map)
        .containsEntry(
            ResourceAttributes.CLOUD_RESOURCE_ID,
            "/subscriptions/foo/resourceGroups/TEST_WEBSITE_RESOURCE_GROUP/providers/Microsoft.Web/sites/TEST_WEBSITE_SITE_NAME");
  }

  @Test
  void noResourceId() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("WEBSITE_RESOURCE_GROUP");

    createResource(map).doesNotContainKey(ResourceAttributes.CLOUD_RESOURCE_ID);
  }

  @Test
  void noWebsite() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("WEBSITE_SITE_NAME");

    createResource(map).isEmpty();
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return OpenTelemetryAssertions.assertThat(
        new AzureAppServiceResourceProvider(map).createResource(null).getAttributes());
  }
}
