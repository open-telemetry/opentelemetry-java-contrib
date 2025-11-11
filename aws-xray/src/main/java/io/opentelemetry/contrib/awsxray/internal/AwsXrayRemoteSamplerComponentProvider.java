/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.internal;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.awsxray.AwsXrayRemoteSampler;
import io.opentelemetry.contrib.awsxray.AwsXrayRemoteSamplerBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.extension.incubator.ExtendedOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.function.Supplier;

/**
 * File configuration SPI implementation for {@link AwsXrayRemoteSampler}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class AwsXrayRemoteSamplerComponentProvider
    implements ComponentProvider<Sampler>, AutoConfigureListener {

  private Resource resource;

  private final Supplier<Resource> resourceSupplier = () -> resource;

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
    AwsXrayRemoteSamplerBuilder builder = AwsXrayRemoteSampler.newBuilder(resourceSupplier);

    String endpoint = config.getString("endpoint");
    if (endpoint != null) {
      builder.setEndpoint(endpoint);
    }

    return builder.build();
  }

  @Override
  public void afterAutoConfigure(OpenTelemetrySdk sdk) {
    if (sdk instanceof ExtendedOpenTelemetrySdk) {
      this.resource = ((ExtendedOpenTelemetrySdk) sdk).getResource();
    }
  }
}
