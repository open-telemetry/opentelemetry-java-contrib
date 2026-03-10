/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.trace.SpanProcessor;

@AutoService(ComponentProvider.class)
public class BaggageSpanComponentProvider implements ComponentProvider {
  @Override
  public String getName() {
    return "baggage";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return new BaggageSpanProcessor(
        IncludeExcludePredicate.createPatternMatching(
            config.getScalarList("included", String.class),
            config.getScalarList("excluded", String.class)));
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
