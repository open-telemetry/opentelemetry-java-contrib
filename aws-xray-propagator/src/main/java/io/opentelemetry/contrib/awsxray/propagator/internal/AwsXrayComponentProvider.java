/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator.internal;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

/**
 * Declarative configuration SPI implementation for {@link AwsXrayPropagator}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class AwsXrayComponentProvider implements ComponentProvider {
  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return "xray";
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties config) {
    return AwsXrayPropagator.getInstance();
  }
}
