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

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
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
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformValues.AZURE_VM);
  }

  private static void assertDefaultAttributes(AttributesAssert attributesAssert) {
    attributesAssert
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformValues.AZURE_VM)
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
