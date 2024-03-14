/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.ResourceAttributes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AzureVmResourceProviderTest {
  @Test
  void success() {
    Resource resource = new AzureVmResourceProvider(() -> Optional.of(read())).createResource(null);
    AttributesAssert attributesAssert =
        OpenTelemetryAssertions.assertThat(resource.getAttributes());

    attributesAssert
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "azure")
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.AZURE_VM)
        .containsEntry(ResourceAttributes.CLOUD_REGION, "westus")
        .containsEntry(
            ResourceAttributes.CLOUD_RESOURCE_ID,
            "/subscriptions/xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx/resourceGroups/macikgo-test-may-23/providers/Microsoft.Compute/virtualMachines/examplevmname")
        .containsEntry(ResourceAttributes.HOST_ID, "02aab8a4-74ef-476e-8182-f6d2ba4166a6")
        .containsEntry(ResourceAttributes.HOST_NAME, "examplevmname")
        .containsEntry(ResourceAttributes.HOST_TYPE, "Standard_A3")
        .containsEntry(ResourceAttributes.OS_TYPE, "Linux")
        .containsEntry(ResourceAttributes.OS_VERSION, "15.05.22")
        .containsEntry("azure.vm.scaleset.name", "crpteste9vflji9")
        .containsEntry("azure.vm.sku", "18.04-LTS");
  }

  private static String read() {
    try {
      return CharStreams.toString(
          new InputStreamReader(
              Objects.requireNonNull(
                  AzureVmResourceProviderTest.class
                      .getClassLoader()
                      .getResourceAsStream("response.json")),
              Charsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
