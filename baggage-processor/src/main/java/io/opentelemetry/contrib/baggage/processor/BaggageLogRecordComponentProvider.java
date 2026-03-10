/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.logs.LogRecordProcessor;

@AutoService(ComponentProvider.class)
public class BaggageLogRecordComponentProvider implements ComponentProvider {
  @Override
  public String getName() {
    return "baggage";
  }

  @Override
  public LogRecordProcessor create(DeclarativeConfigProperties config) {
    return new BaggageLogRecordProcessor(
        IncludeExcludePredicate.createPatternMatching(
            config.getScalarList("included", String.class),
            config.getScalarList("excluded", String.class)));
  }

  @Override
  public Class<LogRecordProcessor> getType() {
    return LogRecordProcessor.class;
  }
}
