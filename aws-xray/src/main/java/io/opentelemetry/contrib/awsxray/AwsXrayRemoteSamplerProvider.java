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
import javax.annotation.Nullable;

@AutoService(ConfigurableSamplerProvider.class)
public class AwsXrayRemoteSamplerProvider implements ConfigurableSamplerProvider {

  @Override
  public Sampler createSampler(ConfigProperties config) {
    Resource resource = ResourceHolder.resource;
    if (resource == null) {
      // Should never be the case in practice.
      resource = Resource.getDefault();
    }
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

  // Currently the only way to read the Resource from autoconfiguration. Best would be if the SPI
  // could return a Function<SamplerFactoryArgs, Sampler> where SamplerFactoryArgs has
  // SDK-constructed components like Resource and Clock.
  @AutoService(AutoConfigurationCustomizerProvider.class)
  public static final class ResourceHolder implements AutoConfigurationCustomizerProvider {

    @Nullable static volatile Resource resource;

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
      autoConfiguration.addResourceCustomizer(
          (resource, config) -> {
            ResourceHolder.resource = resource;
            return resource;
          });
    }
  }
}
