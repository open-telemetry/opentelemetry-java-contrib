/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * File configuration SPI implementation for {@link AwsXrayRemoteSampler}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class AwsXrayRemoteSamplerComponentProvider implements ComponentProvider<Sampler> {
  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "xray";
  }

  @Override
  public Sampler create(DeclarativeConfigProperties config) {
    Resource resource = io.opentelemetry.contrib.awsxray.ResourceHolder.getResource();
    AwsXrayRemoteSamplerBuilder builder = AwsXrayRemoteSampler.newBuilder(resource);

    String endpoint = config.getString("endpoint");
    if (endpoint != null) {
      builder.setEndpoint(endpoint);
    }

    return builder.build();
  }
}
