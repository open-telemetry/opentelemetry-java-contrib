/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class StackTraceComponentProvider implements ComponentProvider<SpanProcessor> {
  @Override
  public String getName() {
    return "experimental-stacktrace";
  }

  @Override
  public SpanProcessor create(DeclarativeConfigProperties config) {
    return StackTraceAutoConfig.create(
        ConfigPropertiesUtil.resolveConfig(
            config,
            ConfigPropertiesUtil.propertyTranslatorBuilder()
                .addTranslation(StackTraceAutoConfig.PREFIX, "")));
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }
}
