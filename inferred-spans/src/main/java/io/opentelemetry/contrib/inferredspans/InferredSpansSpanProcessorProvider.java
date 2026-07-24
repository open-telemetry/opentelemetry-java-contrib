/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.logging.Logger;

@AutoService(ComponentProvider.class)
public class InferredSpansSpanProcessorProvider implements ComponentProvider {

  private static final Logger log =
      Logger.getLogger(InferredSpansSpanProcessorProvider.class.getName());

  @Override
  public String getName() {
    return "inferred_spans/development";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties declarativeConfigProperties) {
    if (InferredSpansConfig.isEnabled(declarativeConfigProperties)) {
      return InferredSpansConfig.createSpanProcessor(declarativeConfigProperties);
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
