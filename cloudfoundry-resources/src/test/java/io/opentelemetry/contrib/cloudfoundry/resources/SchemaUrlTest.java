/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resources;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.event.Level;

class SchemaUrlTest {

  @RegisterExtension
  LogCapturer logs = LogCapturer.create().captureForType(Resource.class, Level.INFO);

  // Fails when an SDK upgrade moves its resource to a different schema version than ours.
  @ParameterizedTest
  @CsvSource({
    "otel.service.name, configured-service",
    "otel.resource.attributes, service.name=configured-service"
  })
  void mergesWithSdkResourceWithoutSchemaConflict(String property, String value) {
    Resource sdkResource = autoConfiguredResource(property, value);
    Map<String, String> env = singletonMap("VCAP_APPLICATION", "{\"application_id\":\"app-id\"}");
    Resource cloudFoundryResource = CloudFoundryResource.buildResource(env::get);

    Resource merged = sdkResource.merge(cloudFoundryResource);

    assertThat(sdkResource.getSchemaUrl()).isEqualTo(cloudFoundryResource.getSchemaUrl());
    assertThat(merged.getSchemaUrl()).isEqualTo(cloudFoundryResource.getSchemaUrl());
    assertThat(logs.getEvents()).isEmpty();
  }

  private static Resource autoConfiguredResource(String property, String value) {
    Map<String, String> properties = new HashMap<>();
    properties.put(property, value);
    properties.put("otel.traces.exporter", "none");
    properties.put("otel.metrics.exporter", "none");
    properties.put("otel.logs.exporter", "none");
    AtomicReference<Resource> configuredResource = new AtomicReference<>();

    OpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(() -> properties)
            .addResourceCustomizer((resource, config) -> {
              configuredResource.set(resource);
              return resource;
            })
            .build()
            .getOpenTelemetrySdk();
    sdk.close();
    return configuredResource.get();
  }
}
