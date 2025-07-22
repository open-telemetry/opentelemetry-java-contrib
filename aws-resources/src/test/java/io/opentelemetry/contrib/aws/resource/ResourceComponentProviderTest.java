/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Test;

class ResourceComponentProviderTest {

  @Test
  @SuppressWarnings("rawtypes")
  void providerIsLoaded() {
    Iterable<ComponentProvider> providers =
        ComponentLoader.forClassLoader(ResourceComponentProviderTest.class.getClassLoader())
            .load(ComponentProvider.class);
    assertThat(providers).extracting(ComponentProvider::getName).containsExactly("aws");
  }

  @Test
  void endToEnd() {
    assertThat(
            AutoConfiguredOpenTelemetrySdk.initialize()
                .getOpenTelemetrySdk()
                .getSdkTracerProvider())
        .extracting("sharedState")
        .extracting("resource")
        .extracting(
            "attributes",
            new InstanceOfAssertFactory<>(Attributes.class, OpenTelemetryAssertions::assertThat))
        .containsEntry(
            CloudIncubatingAttributes.CLOUD_PROVIDER,
            CloudIncubatingAttributes.CloudProviderIncubatingValues.AWS);
  }
}
