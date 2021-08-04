/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigurableSamplerProvider;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class AwsXrayRemoteSamplerProviderTest {

  @Test
  void serviceProvider() {
    ServiceLoader<ConfigurableSamplerProvider> samplerProviders =
        ServiceLoader.load(ConfigurableSamplerProvider.class);
    assertThat(samplerProviders).hasAtLeastOneElementOfType(AwsXrayRemoteSamplerProvider.class);
  }
}
