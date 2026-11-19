/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.internal.ExtendedOpenTelemetrySdk;
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
    ExtendedOpenTelemetrySdk sdk =
        (ExtendedOpenTelemetrySdk)
            AutoConfiguredOpenTelemetrySdk.builder().build().getOpenTelemetrySdk();

    assertThat(sdk.getSdkResourceProvider().getResource().getAttributes())
        .containsEntry("telemetry.distro.name", "opentelemetry-maven-extension");
  }
}
