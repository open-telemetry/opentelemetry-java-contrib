/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class InferredSpansSpanProcessorProvider implements ComponentProvider<SpanProcessor> {

  private static final String PREFIX = "otel.inferred.spans.";

  @Override
  public String getName() {
    return "experimental_inferred_spans";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    DeclarativeConfigPropertiesBridgeBuilder builder =
        new DeclarativeConfigPropertiesBridgeBuilder();

    for (String property : InferredSpansConfig.ALL_PROPERTIES) {
      // 1. crop the prefix, because the properties are under the "experimental_inferred_spans"
      // 2. we want all properties flat under "otel.inferred.spans.*"
      builder.addMapping(property, property.substring(PREFIX.length()).replace('.', '_'));
    }

    return InferredSpansConfig.createSpanProcessor(builder.build(config));
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
