/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION;

import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import java.util.Optional;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

class AzureVmResourceProviderTest extends MetadataBasedResourceProviderTest {
  @NotNull
  @Override
  protected ResourceProvider getResourceProvider(Supplier<Optional<String>> client) {
    return new AzureVmResourceProvider(client);
  }

  @Override
  protected String getPlatform() {
    return CloudIncubatingAttributes.CloudPlatformIncubatingValues.AZURE_VM;
  }

  @Override
  protected void assertDefaultAttributes(AttributesAssert attributesAssert) {
    attributesAssert
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(
            CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AZURE_VM)
        .containsEntry(CLOUD_REGION, "westus")
        .containsEntry(
            CLOUD_RESOURCE_ID,
            "/subscriptions/xxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx/resourceGroups/macikgo-test-may-23/providers/Microsoft.Compute/virtualMachines/examplevmname")
        .containsEntry(HOST_ID, "02aab8a4-74ef-476e-8182-f6d2ba4166a6")
        .containsEntry(HOST_NAME, "examplevmname")
        .containsEntry(HOST_TYPE, "Standard_A3")
        .containsEntry(OS_TYPE, "Linux")
        .containsEntry(OS_VERSION, "15.05.22")
        .containsEntry("azure.vm.scaleset.name", "crpteste9vflji9")
        .containsEntry("azure.vm.sku", "18.04-LTS");
  }
}
