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

class AzureFunctionsResourceProviderTest {
  private static final String TEST_WEBSITE_SITE_NAME = "TEST_WEBSITE_SITE_NAME";
  private static final String TEST_REGION_NAME = "TEST_REGION_NAME";
  private static final String TEST_FUNCTION_VERSION = "TEST_VERSION";
  private static final String TEST_WEBSITE_INSTANCE_ID = "TEST_WEBSITE_INSTANCE_ID";
  private static final String TEST_MEM_LIMIT = "1024";
  private static final ImmutableMap<String, String> DEFAULT_ENV_VARS =
      ImmutableMap.of(
          "WEBSITE_SITE_NAME", TEST_WEBSITE_SITE_NAME,
          "REGION_NAME", TEST_REGION_NAME,
          "WEBSITE_MEMORY_LIMIT_MB", TEST_MEM_LIMIT,
          "FUNCTIONS_EXTENSION_VERSION", TEST_FUNCTION_VERSION,
          "WEBSITE_INSTANCE_ID", TEST_WEBSITE_INSTANCE_ID);

  @Test
  void defaultValues() {
    createResource(DEFAULT_ENV_VARS)
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "azure")
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "azure_functions")
        .containsEntry(ResourceAttributes.FAAS_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(ResourceAttributes.FAAS_VERSION, TEST_FUNCTION_VERSION)
        .containsEntry(ResourceAttributes.FAAS_INSTANCE, TEST_WEBSITE_INSTANCE_ID)
        .containsEntry(ResourceAttributes.FAAS_MAX_MEMORY, Long.parseLong(TEST_MEM_LIMIT));
  }

  @Test
  void isNotFunction() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("FUNCTIONS_EXTENSION_VERSION");

    createResource(map).isEmpty();
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return OpenTelemetryAssertions.assertThat(
        new AzureFunctionsResourceProvider(map).createResource(null).getAttributes());
  }
}
