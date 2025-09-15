/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes.SERVICE_INSTANCE_ID;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class AzureContainersResourceProviderTest {
  private static final String TEST_APP_NAME = "TEST_APP_NAME";
  private static final String TEST_REPLICA_NAME = "TEST_REPLICA_NAME";
  private static final String TEST_REVISION = "TEST_REVISION";

  private static final ImmutableMap<String, String> DEFAULT_ENV_VARS =
      ImmutableMap.of(
          "CONTAINER_APP_NAME", TEST_APP_NAME,
          "CONTAINER_APP_REPLICA_NAME", TEST_REPLICA_NAME,
          "CONTAINER_APP_REVISION", TEST_REVISION);

  @Test
  void defaultValues() {
    createResource(DEFAULT_ENV_VARS)
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, "azure_container_apps")
        .containsEntry(SERVICE_NAME, TEST_APP_NAME)
        .containsEntry(SERVICE_INSTANCE_ID, TEST_REPLICA_NAME)
        .containsEntry(SERVICE_VERSION, TEST_REVISION);
  }

  @Test
  void isNotContainer() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("CONTAINER_APP_NAME");

    createResource(map).isEmpty();
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return assertThat(
        new AzureContainersResourceProvider(map).createResource(null).getAttributes());
  }
}
