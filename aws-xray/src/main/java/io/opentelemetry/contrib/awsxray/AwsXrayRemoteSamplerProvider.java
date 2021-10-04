/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetryResourceAutoConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;

@AutoService(ConfigurableSamplerProvider.class)
public class AwsXrayRemoteSamplerProvider implements ConfigurableSamplerProvider {

  @Override
  public Sampler createSampler(ConfigProperties config) {
    AwsXrayRemoteSamplerBuilder builder =
        AwsXrayRemoteSampler.newBuilder(
            OpenTelemetryResourceAutoConfiguration.configureResource(config));

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
}
