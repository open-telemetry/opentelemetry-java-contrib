/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Collections;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class BaggageSpanComponentProvider implements ComponentProvider<SpanProcessor> {
  @Override
  public String getName() {
    return "baggage";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return BaggageProcessorCustomizer.createBaggageSpanProcessor(
        ConfigPropertiesUtil.resolveConfig(
            config, Collections.singletonMap(BaggageProcessorCustomizer.SPAN_PREFIX, "")));
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
