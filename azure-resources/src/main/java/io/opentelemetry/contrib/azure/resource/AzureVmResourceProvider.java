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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class AzureVmResourceProvider extends CloudResourceProvider {

  private static final Map<String, AttributeKey<String>> COMPUTE_MAPPING = new HashMap<>();

  static {
    COMPUTE_MAPPING.put("location", CLOUD_REGION);
    COMPUTE_MAPPING.put("resourceId", CLOUD_RESOURCE_ID);
    COMPUTE_MAPPING.put("vmId", HOST_ID);
    COMPUTE_MAPPING.put("name", HOST_NAME);
    COMPUTE_MAPPING.put("vmSize", HOST_TYPE);
    COMPUTE_MAPPING.put("osType", OS_TYPE);
    COMPUTE_MAPPING.put("version", OS_VERSION);
    COMPUTE_MAPPING.put("vmScaleSetName", AttributeKey.stringKey("azure.vm.scaleset.name"));
    COMPUTE_MAPPING.put("sku", AttributeKey.stringKey("azure.vm.sku"));
  }

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
        azureAttributeBuilder(CloudIncubatingAttributes.CloudPlatformValues.AZURE_VM);
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
    builder.put(CLOUD_PROVIDER, CloudIncubatingAttributes.CloudProviderValues.AZURE);
    builder.put(CLOUD_PLATFORM, platform);
    return builder;
  }

  static void parseResponse(JsonParser parser, AttributesBuilder builder) throws IOException {
    if (!parser.isExpectedStartObjectToken()) {
      logger.log(Level.FINE, "Couldn't parse ECS metadata, invalid JSON");
      return;
    }

    consumeJson(
        parser,
        (name, value) -> {
          try {
            if (name.equals("compute")) {
              consumeCompute(parser, builder);
            } else {
              parser.skipChildren();
            }
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
  }

  private static void consumeCompute(JsonParser parser, AttributesBuilder builder)
      throws IOException {
    consumeJson(
        parser,
        (computeName, computeValue) -> {
          AttributeKey<String> key = COMPUTE_MAPPING.get(computeName);
          if (key != null) {
            builder.put(key, computeValue);
          } else {
            try {
              parser.skipChildren();
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });
  }

  private static void consumeJson(JsonParser parser, BiConsumer<String, String> consumer)
      throws IOException {
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      consumer.accept(parser.currentName(), parser.nextTextValue());
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
