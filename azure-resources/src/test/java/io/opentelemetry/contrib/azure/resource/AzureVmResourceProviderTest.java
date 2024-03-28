/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.ResourceAttributes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AzureVmResourceProviderTest {

  @RegisterExtension
  public static final MockWebServerExtension server = new MockWebServerExtension();

  @Test
  void successFromFile() {
    assertDefaultAttributes(createResource(() -> Optional.of(okResponse())));
  }

  @Test
  void successFromMockServer() {
    server.enqueue(HttpResponse.of(MediaType.JSON, okResponse()));
    assertDefaultAttributes(mockServerResponse());
  }

  @Test
  void responseNotFound() {
    server.enqueue(HttpResponse.of(HttpStatus.NOT_FOUND));
    mockServerResponse().isEmpty();
  }

  @Test
  void responseEmpty() {
    server.enqueue(HttpResponse.of(""));
    assertOnlyProvider(mockServerResponse());
  }

  @Test
  void responseEmptyJson() {
    server.enqueue(HttpResponse.of("{}"));
    assertOnlyProvider(mockServerResponse());
  }

  @NotNull
  private static AttributesAssert mockServerResponse() {
    return createResource(
        () -> {
          try {
            return AzureVmResourceProvider.fetchMetadata(server.httpUri().toURL());
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @NotNull
  private static AttributesAssert createResource(Supplier<Optional<String>> client) {
    Resource resource = new AzureVmResourceProvider(client).createResource(null);
    return OpenTelemetryAssertions.assertThat(resource.getAttributes());
  }

  private static void assertOnlyProvider(AttributesAssert attributesAssert) {
    attributesAssert
        .hasSize(2)
        .containsEntry(ResourceAttributes.CLOUD_PROVIDER, "azure")
        .containsEntry(
            ResourceAttributes.CLOUD_PLATFORM, ResourceAttributes.CloudPlatformValues.AZURE_VM);
  }

  private static void assertDefaultAttributes(AttributesAssert attributesAssert) {
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

  private static String okResponse() {
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
