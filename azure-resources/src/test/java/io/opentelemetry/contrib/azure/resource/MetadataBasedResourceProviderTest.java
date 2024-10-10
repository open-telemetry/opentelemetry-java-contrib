/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class MetadataBasedResourceProviderTest {
  @RegisterExtension
  public static final MockWebServerExtension server = new MockWebServerExtension();

  @NotNull
  private AttributesAssert mockServerResponse() {
    return createResource(
        () -> {
          try {
            return AzureMetadataService.fetchMetadata(server.httpUri().toURL());
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @NotNull
  private AttributesAssert createResource(Supplier<Optional<String>> client) {
    Resource resource = getResourceProvider(client).createResource(null);
    return OpenTelemetryAssertions.assertThat(resource.getAttributes());
  }

  @NotNull
  protected abstract ResourceProvider getResourceProvider(Supplier<Optional<String>> client);

  private void assertOnlyProvider(AttributesAssert attributesAssert) {
    attributesAssert
        .hasSize(2)
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(CLOUD_PLATFORM, getPlatform());
  }

  protected abstract String getPlatform();

  protected abstract void assertDefaultAttributes(AttributesAssert attributesAssert);

  protected static String okResponse() {
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

  @Test
  public void successFromFile() {
    assertDefaultAttributes(createResource(() -> Optional.of(okResponse())));
  }

  @Test
  public void successFromMockServer() {
    server.enqueue(HttpResponse.of(MediaType.JSON, okResponse()));
    assertDefaultAttributes(mockServerResponse());
  }

  @Test
  public void responseNotFound() {
    server.enqueue(HttpResponse.of(HttpStatus.NOT_FOUND));
    mockServerResponse().isEmpty();
  }

  @Test
  public void responseEmpty() {
    server.enqueue(HttpResponse.of(""));
    assertOnlyProvider(mockServerResponse());
  }

  @Test
  public void responseEmptyJson() {
    server.enqueue(HttpResponse.of("{}"));
    assertOnlyProvider(mockServerResponse());
  }
}
