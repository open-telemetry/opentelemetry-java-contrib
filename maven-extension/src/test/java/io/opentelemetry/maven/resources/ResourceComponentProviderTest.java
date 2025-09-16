/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {

  @Test
  @SuppressWarnings("rawtypes")
  void providerIsLoaded() {
    Iterable<ComponentProvider> providers =
        ComponentLoader.forClassLoader(ResourceComponentProviderTest.class.getClassLoader())
            .load(ComponentProvider.class);
    assertThat(providers).extracting(ComponentProvider::getName).contains("maven");
  }

  @Test
  void endToEnd() {
    assertThat(
            AutoConfiguredOpenTelemetrySdk.builder()
                .build()
                .getOpenTelemetrySdk()
                .getSdkTracerProvider())
        .extracting("sharedState")
        .extracting("resource")
        .extracting(
            "attributes",
            new InstanceOfAssertFactory<>(Attributes.class, OpenTelemetryAssertions::assertThat))
        .containsEntry("telemetry.distro.name", "opentelemetry-maven-extension");
  }
}
