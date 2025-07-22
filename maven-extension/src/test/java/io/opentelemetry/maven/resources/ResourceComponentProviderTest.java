/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
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
}
