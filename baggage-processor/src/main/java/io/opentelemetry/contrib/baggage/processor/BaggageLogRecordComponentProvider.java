/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import java.util.Collections;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class BaggageLogRecordComponentProvider implements ComponentProvider<LogRecordProcessor> {
  @Override
  public String getName() {
    return "baggage";
  }

  @Override
  public LogRecordProcessor create(DeclarativeConfigProperties config) {
    return BaggageProcessorCustomizer.createBaggageLogRecordProcessor(
        ConfigPropertiesUtil.resolveConfig(
            config, Collections.singletonMap(BaggageProcessorCustomizer.LOG_PREFIX, "")));
  }

  @Override
  public Class<LogRecordProcessor> getType() {
    return LogRecordProcessor.class;
  }
}
