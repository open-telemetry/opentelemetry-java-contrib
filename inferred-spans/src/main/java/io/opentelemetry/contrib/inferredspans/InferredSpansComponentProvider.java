/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.sdk.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class InferredSpansComponentProvider implements ComponentProvider<SpanProcessor> {

  @Override
  public String getName() {
    return "inferred_spans";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return InferredSpansAutoConfig.create(
        new DeclarativeConfigPropertiesBridgeBuilder()
            // crop the prefix, because the properties are under the "inferred_spans" processor
            .addMapping("otel.inferred.spans.", "")
            .build(config));
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
