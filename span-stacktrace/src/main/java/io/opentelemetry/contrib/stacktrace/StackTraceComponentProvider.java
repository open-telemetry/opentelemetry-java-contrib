/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@AutoService(ComponentProvider.class)
public class StackTraceComponentProvider implements ComponentProvider {
  @Override
  public String getName() {
    return "stacktrace/development";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return StackTraceAutoConfig.create(config);
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
