/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray.propagator.internal;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class AwsXrayComponentProvider implements ComponentProvider<TextMapPropagator> {
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
