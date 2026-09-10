/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.DeploymentAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.AzureIncubatingAttributes.AZURE_RESOURCE_GROUP_NAME;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_INSTANCE;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_MAX_MEMORY;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_NAME;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_VERSION;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class AzureFunctionsResourceProviderTest {
  private static final String TEST_WEBSITE_SITE_NAME = "TEST_WEBSITE_SITE_NAME";
  private static final String TEST_REGION_NAME = "TEST_REGION_NAME";
  private static final String TEST_WEBSITE_SLOT_NAME = "TEST_WEBSITE_SLOT_NAME";
  private static final String TEST_FUNCTION_VERSION = "TEST_VERSION";
  private static final String TEST_WEBSITE_INSTANCE_ID = "TEST_WEBSITE_INSTANCE_ID";
  private static final String TEST_MEM_LIMIT = "1024";
  private static final String TEST_WEBSITE_RESOURCE_GROUP = "TEST_WEBSITE_RESOURCE_GROUP";
  private static final String TEST_WEBSITE_OWNER_NAME = "TEST_SUBSCRIPTION_ID+TEST_OWNER";
  private static final ImmutableMap<String, String> DEFAULT_ENV_VARS =
      ImmutableMap.of(
          "WEBSITE_SITE_NAME", TEST_WEBSITE_SITE_NAME,
          "REGION_NAME", TEST_REGION_NAME,
          "WEBSITE_SLOT_NAME", TEST_WEBSITE_SLOT_NAME,
          "WEBSITE_MEMORY_LIMIT_MB", TEST_MEM_LIMIT,
          "FUNCTIONS_EXTENSION_VERSION", TEST_FUNCTION_VERSION,
          "WEBSITE_INSTANCE_ID", TEST_WEBSITE_INSTANCE_ID,
          "WEBSITE_RESOURCE_GROUP", TEST_WEBSITE_RESOURCE_GROUP,
          "WEBSITE_OWNER_NAME", TEST_WEBSITE_OWNER_NAME);

  @Test
  void defaultValues() {
    createResource(DEFAULT_ENV_VARS)
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, "azure.functions")
        .containsEntry(CLOUD_ACCOUNT_ID, "TEST_SUBSCRIPTION_ID")
        .containsEntry(AZURE_RESOURCE_GROUP_NAME, TEST_WEBSITE_RESOURCE_GROUP)
        .containsEntry(
            CLOUD_RESOURCE_ID,
            "/subscriptions/TEST_SUBSCRIPTION_ID/resourceGroups/TEST_WEBSITE_RESOURCE_GROUP/providers/Microsoft.Web/sites/TEST_WEBSITE_SITE_NAME")
        .containsEntry(DEPLOYMENT_ENVIRONMENT_NAME, TEST_WEBSITE_SLOT_NAME)
        .containsEntry(SERVICE_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(FAAS_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(FAAS_VERSION, TEST_FUNCTION_VERSION)
        .containsEntry(FAAS_INSTANCE, TEST_WEBSITE_INSTANCE_ID)
        .containsEntry(FAAS_MAX_MEMORY, Long.parseLong(TEST_MEM_LIMIT));
  }

  @Test
  void noResourceGroup() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("WEBSITE_RESOURCE_GROUP");

    createResource(map)
        .containsEntry(CLOUD_ACCOUNT_ID, "TEST_SUBSCRIPTION_ID")
        .containsEntry(SERVICE_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(FAAS_NAME, TEST_WEBSITE_SITE_NAME)
        .doesNotContainKey(AZURE_RESOURCE_GROUP_NAME)
        .doesNotContainKey(CLOUD_RESOURCE_ID);
  }

  @Test
  void noOwner() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("WEBSITE_OWNER_NAME");

    createResource(map)
        .containsEntry(AZURE_RESOURCE_GROUP_NAME, TEST_WEBSITE_RESOURCE_GROUP)
        .containsEntry(SERVICE_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(FAAS_NAME, TEST_WEBSITE_SITE_NAME)
        .doesNotContainKey(CLOUD_ACCOUNT_ID)
        .doesNotContainKey(CLOUD_RESOURCE_ID);
  }

  @Test
  void malformedOwnerNameNoPlus() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.put("WEBSITE_OWNER_NAME", "TEST_OWNER_NO_PLUS");

    createResource(map)
        .containsEntry(AZURE_RESOURCE_GROUP_NAME, TEST_WEBSITE_RESOURCE_GROUP)
        .containsEntry(SERVICE_NAME, TEST_WEBSITE_SITE_NAME)
        .doesNotContainKey(CLOUD_ACCOUNT_ID)
        .doesNotContainKey(CLOUD_RESOURCE_ID);
  }

  @Test
  void malformedOwnerNamePlusAtIndexZero() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.put("WEBSITE_OWNER_NAME", "+TEST_OWNER");

    createResource(map)
        .containsEntry(AZURE_RESOURCE_GROUP_NAME, TEST_WEBSITE_RESOURCE_GROUP)
        .doesNotContainKey(CLOUD_ACCOUNT_ID)
        .doesNotContainKey(CLOUD_RESOURCE_ID);
  }

  @Test
  void malformedOwnerNameEmptySubscriptionSegment() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.put("WEBSITE_OWNER_NAME", "+");

    createResource(map)
        .containsEntry(AZURE_RESOURCE_GROUP_NAME, TEST_WEBSITE_RESOURCE_GROUP)
        .doesNotContainKey(CLOUD_ACCOUNT_ID)
        .doesNotContainKey(CLOUD_RESOURCE_ID);
  }

  @Test
  void noWebsiteSlot() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("WEBSITE_SLOT_NAME");

    createResource(map).doesNotContainKey(DEPLOYMENT_ENVIRONMENT_NAME);
  }

  @Test
  void isNotFunction() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("FUNCTIONS_EXTENSION_VERSION");

    createResource(map).isEmpty();
  }

  @Test
  void malformedMemoryLimit() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.put("WEBSITE_MEMORY_LIMIT_MB", "not-a-number");

    createResource(map)
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, "azure.functions")
        .doesNotContainKey(FAAS_MAX_MEMORY);
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return assertThat(new AzureFunctionsResourceProvider(map).createResource(null).getAttributes());
  }
}
