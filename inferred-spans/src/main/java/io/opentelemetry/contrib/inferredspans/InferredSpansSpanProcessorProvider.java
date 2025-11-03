/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class InferredSpansSpanProcessorProvider implements ComponentProvider<SpanProcessor> {

  private static final Logger log =
      Logger.getLogger(InferredSpansSpanProcessorProvider.class.getName());

  private static final String PREFIX = "otel.inferred.spans.";

  @Override
  public String getName() {
    return "inferred_spans/development";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    DeclarativeConfigPropertiesBridgeBuilder builder =
        new DeclarativeConfigPropertiesBridgeBuilder();

    for (String property : InferredSpansConfig.ALL_PROPERTIES) {
      // 1. crop the prefix, because the properties are under the "inferred_spans/development"
      // 2. we want all properties flat under "otel.inferred.spans.*"
      builder.addMapping(property, property.substring(PREFIX.length()).replace('.', '_'));
    }

    ConfigProperties properties = builder.build(declarativeConfigProperties);
    if (properties.getBoolean(InferredSpansConfig.ENABLED_OPTION, true)) {
      return InferredSpansConfig.createSpanProcessor(properties);
    } else {
      log.finest("Not enabling inferred spans processor because enabled=false");
      return SpanProcessor.composite();
    }
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
