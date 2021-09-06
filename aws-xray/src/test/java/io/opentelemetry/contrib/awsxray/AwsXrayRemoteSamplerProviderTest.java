/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurableSamplerProvider;
import java.util.Collections;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwsXrayRemoteSamplerProviderTest {

  @Mock private ConfigProperties config;

  @Test
  void serviceProvider() {
    ServiceLoader<ConfigurableSamplerProvider> samplerProviders =
        ServiceLoader.load(ConfigurableSamplerProvider.class);
    assertThat(samplerProviders)
        .singleElement(type(AwsXrayRemoteSamplerProvider.class))
        .satisfies(
            provider -> {
              assertThat(provider.getName()).isEqualTo("xray");
            });
  }

  @Test
  void emptyConfig() {
    try (AwsXrayRemoteSampler sampler =
        (AwsXrayRemoteSampler) new AwsXrayRemoteSamplerProvider().createSampler(config)) {
      // Inspect implementation details for simplicity, otherwise we'd probably need to make a
      // test HTTP server that records requests.
      assertThat(sampler)
          .extracting("client")
          .extracting("getSamplingRulesEndpoint", type(String.class))
          .isEqualTo("http://localhost:2000/GetSamplingRules");
    }
  }

  @Test
  void setEndpoint() {
    when(config.getCommaSeparatedMap("otel.resource.attributes"))
        .thenReturn(Collections.emptyMap());
    when(config.getCommaSeparatedMap("otel.traces.sampler.arg"))
        .thenReturn(Collections.singletonMap("endpoint", "http://localhost:3000"));
    try (AwsXrayRemoteSampler sampler =
        (AwsXrayRemoteSampler) new AwsXrayRemoteSamplerProvider().createSampler(config)) {
      // Inspect implementation details for simplicity, otherwise we'd probably need to make a
      // test HTTP server that records requests.
      assertThat(sampler)
          .extracting("client")
          .extracting("getSamplingRulesEndpoint", type(String.class))
          .isEqualTo("http://localhost:3000/GetSamplingRules");
    }
  }
}
