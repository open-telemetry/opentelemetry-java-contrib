/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AZURE_VM;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CloudProviderIncubatingValues.AZURE;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.HOST_ID;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.HOST_NAME;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.HOST_TYPE;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.OS_TYPE;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.OS_VERSION;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public final class AzureVmResourceProvider extends CloudResourceProvider {

  private static final Map<String, Entry> COMPUTE_MAPPING = new HashMap<>();

  static {
    COMPUTE_MAPPING.put("location", new Entry(CLOUD_REGION));
    COMPUTE_MAPPING.put("resourceId", new Entry(CLOUD_RESOURCE_ID));
    COMPUTE_MAPPING.put("vmId", new Entry(HOST_ID));
    COMPUTE_MAPPING.put("name", new Entry(HOST_NAME));
    COMPUTE_MAPPING.put("vmSize", new Entry(HOST_TYPE));
    COMPUTE_MAPPING.put("osType", new Entry(OS_TYPE));
    COMPUTE_MAPPING.put("version", new Entry(OS_VERSION));
    COMPUTE_MAPPING.put(
        "vmScaleSetName", new Entry(AttributeKey.stringKey("azure.vm.scaleset.name")));
    COMPUTE_MAPPING.put("sku", new Entry(AttributeKey.stringKey("azure.vm.sku")));
  }

  private static final Logger logger = Logger.getLogger(AzureVmResourceProvider.class.getName());

  private final Supplier<Optional<String>> client;

  // SPI
  public AzureVmResourceProvider() {
    this(AzureMetadataService.defaultClient());
  }

  // visible for testing
  AzureVmResourceProvider(Supplier<Optional<String>> client) {
    this.client = client;
  }

  @Override
  public int order() {
    // run after the fast cloud resource providers that only check environment variables
    // and after the AKS provider
    return 100;
  }

  @Override
  public Resource createResource() {
    return client
        .get()
        .map(body -> parseMetadata(body, COMPUTE_MAPPING, AZURE_VM))
        .orElse(Resource.empty());
  }

  static Resource parseMetadata(String body, Map<String, Entry> computeMapping, String platform) {
    AttributesBuilder builder = azureAttributeBuilder(platform);
    try (JsonParser parser = AzureMetadataService.JSON_FACTORY.createParser(body)) {
      parser.nextToken();
      parseResponse(parser, builder, computeMapping);
    } catch (IOException e) {
      logger.log(Level.FINE, "Can't get Azure VM metadata", e);
    }
    return Resource.create(builder.build());
  }

  @NotNull
  static AttributesBuilder azureAttributeBuilder(String platform) {
    AttributesBuilder builder = Attributes.builder();
    builder.put(CLOUD_PROVIDER, AZURE);
    builder.put(CLOUD_PLATFORM, platform);
    return builder;
  }

  static void parseResponse(
      JsonParser parser, AttributesBuilder builder, Map<String, Entry> computeMapping)
      throws IOException {
    if (!parser.isExpectedStartObjectToken()) {
      logger.log(Level.FINE, "Couldn't parse ECS metadata, invalid JSON");
      return;
    }

    consumeJson(
        parser,
        (name, value) -> {
          try {
            if (name.equals("compute")) {
              consumeCompute(parser, builder, computeMapping);
            } else {
              parser.skipChildren();
            }
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        });
  }

  private static void consumeCompute(
      JsonParser parser, AttributesBuilder builder, Map<String, Entry> computeMapping)
      throws IOException {
    consumeJson(
        parser,
        (computeName, computeValue) -> {
          Entry entry = computeMapping.get(computeName);
          if (entry != null) {
            builder.put(entry.key, entry.transform.apply(computeValue));
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

  static class Entry {
    final AttributeKey<String> key;
    final Function<String, String> transform;

    Entry(AttributeKey<String> key) {
      this(key, Function.identity());
    }

    Entry(AttributeKey<String> key, Function<String, String> transform) {
      this.key = key;
      this.transform = transform;
    }
  }
}
