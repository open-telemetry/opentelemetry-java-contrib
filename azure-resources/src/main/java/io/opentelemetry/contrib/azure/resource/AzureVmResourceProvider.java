/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class AzureVmResourceProvider extends CloudResourceProvider {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final Duration TIMEOUT = Duration.ofSeconds(1);

  private static final Logger logger = Logger.getLogger(AzureVmResourceProvider.class.getName());
  private static final URL METADATA_URL;

  static {
    try {
      METADATA_URL = new URL("http://169.254.169.254/metadata/instance?api-version=2021-02-01");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private final Supplier<Optional<String>> client;

  // SPI
  public AzureVmResourceProvider() {
    this(() -> fetchMetadata(METADATA_URL));
  }

  // visible for testing
  public AzureVmResourceProvider(Supplier<Optional<String>> client) {
    this.client = client;
  }

  @Override
  public int order() {
    // run after the fast cloud resource providers that only check environment variables
    return 100;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return client.get().map(AzureVmResourceProvider::parseMetadata).orElse(Resource.empty());
  }

  private static Resource parseMetadata(String body) {
    AttributesBuilder builder =
        azureAttributeBuilder(ResourceAttributes.CloudPlatformValues.AZURE_VM);
    try (JsonParser parser = JSON_FACTORY.createParser(body)) {
      parser.nextToken();
      parseResponse(parser, builder);
    } catch (IOException e) {
      logger.log(Level.FINE, "Can't get Azure VM metadata", e);
    }
    return Resource.create(builder.build());
  }

  @NotNull
  static AttributesBuilder azureAttributeBuilder(String platform) {
    AttributesBuilder builder = Attributes.builder();
    builder.put(ResourceAttributes.CLOUD_PROVIDER, ResourceAttributes.CloudProviderValues.AZURE);
    builder.put(ResourceAttributes.CLOUD_PLATFORM, platform);
    return builder;
  }

  static void parseResponse(JsonParser parser, AttributesBuilder builder) throws IOException {
    if (!parser.isExpectedStartObjectToken()) {
      logger.log(Level.FINE, "Couldn't parse ECS metadata, invalid JSON");
      return;
    }

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String value = parser.nextTextValue();
      switch (parser.currentName()) {
        case "compute":
          // go inside
          break;
        case "location":
          builder.put(ResourceAttributes.CLOUD_REGION, value);
          break;
        case "resourceId":
          builder.put(ResourceAttributes.CLOUD_RESOURCE_ID, value);
          break;
        case "vmId":
          builder.put(ResourceAttributes.HOST_ID, value);
          break;
        case "name":
          builder.put(ResourceAttributes.HOST_NAME, value);
          break;
        case "vmSize":
          builder.put(ResourceAttributes.HOST_TYPE, value);
          break;
        case "osType":
          builder.put(ResourceAttributes.OS_TYPE, value);
          break;
        case "version":
          builder.put(ResourceAttributes.OS_VERSION, value);
          break;
        case "vmScaleSetName":
          builder.put(AttributeKey.stringKey("azure.vm.scaleset.name"), value);
          break;
        case "sku":
          builder.put(AttributeKey.stringKey("azure.vm.sku"), value);
          break;
        default:
          parser.skipChildren();
          break;
      }
    }
  }

  // visible for testing
  static Optional<String> fetchMetadata(URL url) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .callTimeout(TIMEOUT)
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT)
            .build();

    Request request = new Request.Builder().url(url).get().addHeader("Metadata", "true").build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();
      if (responseCode != 200) {
        logger.log(
            Level.FINE,
            "Error response from "
                + url
                + " code ("
                + responseCode
                + ") text "
                + response.message());
        return Optional.empty();
      }

      return Optional.of(Objects.requireNonNull(response.body()).string());
    } catch (IOException e) {
      logger.log(Level.FINE, "Failed to fetch Azure VM metadata", e);
      return Optional.empty();
    }
  }
}
