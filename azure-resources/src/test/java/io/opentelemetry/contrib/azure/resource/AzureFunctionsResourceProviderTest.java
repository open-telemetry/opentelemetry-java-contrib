/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
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
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, "azure.functions")
        .containsEntry(FAAS_NAME, TEST_WEBSITE_SITE_NAME)
        .containsEntry(FAAS_VERSION, TEST_FUNCTION_VERSION)
        .containsEntry(FAAS_INSTANCE, TEST_WEBSITE_INSTANCE_ID)
        .containsEntry(FAAS_MAX_MEMORY, Long.parseLong(TEST_MEM_LIMIT));
  }

  @Test
  void isNotFunction() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("FUNCTIONS_EXTENSION_VERSION");

    createResource(map).isEmpty();
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return assertThat(new AzureFunctionsResourceProvider(map).createResource(null).getAttributes());
  }
}
