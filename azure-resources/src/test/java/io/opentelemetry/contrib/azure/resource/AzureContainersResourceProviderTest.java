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
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "azure")
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM, "azure_container_apps")
        .containsEntry(ResourceAttributes.SERVICE_NAME, TEST_APP_NAME)
        .containsEntry(ResourceAttributes.SERVICE_INSTANCE_ID, TEST_REPLICA_NAME)
        .containsEntry(ResourceAttributes.SERVICE_VERSION, TEST_REVISION);
  }

  @Test
  void isNotContainer() {
    HashMap<String, String> map = new HashMap<>(DEFAULT_ENV_VARS);
    map.remove("CONTAINER_APP_NAME");

    createResource(map).isEmpty();
  }

  @NotNull
  private static AttributesAssert createResource(Map<String, String> map) {
    return OpenTelemetryAssertions.assertThat(
        new AzureContainersResourceProvider(map).createResource(null).getAttributes());
  }
}
