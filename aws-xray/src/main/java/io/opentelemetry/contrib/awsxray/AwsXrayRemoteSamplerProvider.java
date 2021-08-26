/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;

@AutoService(ConfigurableSamplerProvider.class)
public class AwsXrayRemoteSamplerProvider implements ConfigurableSamplerProvider {

  @Override
  public Sampler createSampler(ConfigProperties config) {
    AwsXrayRemoteSamplerBuilder builder =
        AwsXrayRemoteSampler.newBuilder(OpenTelemetrySdkAutoConfiguration.getResource());

    Map<String, String> params = config.getCommaSeparatedMap("otel.traces.sampler.arg");

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
