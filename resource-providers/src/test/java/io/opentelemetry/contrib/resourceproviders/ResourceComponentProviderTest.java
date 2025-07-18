/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ResourceComponentProviderTest {

  @Test
  @SuppressWarnings("rawtypes")
  void providerIsLoaded() {
    List<ComponentProvider> providers =
        SpiHelper.create(ResourceComponentProviderTest.class.getClassLoader())
            .load(ComponentProvider.class);
    assertThat(providers).extracting(ComponentProvider::getName).contains("app_server");
  }
}
