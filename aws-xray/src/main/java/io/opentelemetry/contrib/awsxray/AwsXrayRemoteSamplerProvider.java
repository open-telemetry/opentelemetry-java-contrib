/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;

@AutoService(ConfigurableSamplerProvider.class)
public final class AwsXrayRemoteSamplerProvider implements ConfigurableSamplerProvider {

  @Override
  public Sampler createSampler(ConfigProperties config) {
    Resource resource = io.opentelemetry.contrib.awsxray.ResourceHolder.getResource();
    AwsXrayRemoteSamplerBuilder builder = AwsXrayRemoteSampler.newBuilder(resource);

    Map<String, String> params = config.getMap("otel.traces.sampler.arg");

    String endpoint = params.get("endpoint");
    if (endpoint != null) {
      builder.setEndpoint(endpoint);
    }

    return builder.build();
  }

  @Override
  public String getName() {
    return "xray";
  }

  /** Deprecated in favor of {@link io.opentelemetry.contrib.awsxray.ResourceHolder}. */
  @Deprecated
  @AutoService(AutoConfigurationCustomizerProvider.class)
  public static final class ResourceHolder implements AutoConfigurationCustomizerProvider {

    @Deprecated
    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
      // No-op
    }
  }
}
