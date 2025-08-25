/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator.internal;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayLambdaPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

/**
 * A {@link ConfigurablePropagatorProvider} which allows enabling the {@link
 * AwsXrayLambdaPropagator} with the propagator name {@code xray-lambda}.
 */
@AutoService(ConfigurablePropagatorProvider.class)
public class AwsXrayLambdaConfigurablePropagator implements ConfigurablePropagatorProvider {
  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return AwsXrayLambdaPropagator.getInstance();
  }

  @Override
  public String getName() {
    return "xray-lambda";
  }
}
