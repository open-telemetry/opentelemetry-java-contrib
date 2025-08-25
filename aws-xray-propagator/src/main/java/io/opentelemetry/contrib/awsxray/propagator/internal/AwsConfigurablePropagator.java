/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator.internal;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

/**
 * A {@link ConfigurablePropagatorProvider} which allows enabling the {@link AwsXrayPropagator} with
 * the propagator name {@code xray}.
 */
@AutoService(ConfigurablePropagatorProvider.class)
public class AwsConfigurablePropagator implements ConfigurablePropagatorProvider {
  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return AwsXrayPropagator.getInstance();
  }

  @Override
  public String getName() {
    return "xray";
  }
}
