/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.List;

@AutoService(AutoConfigurationCustomizerProvider.class)
public final class BaggageProcessorCustomizer implements AutoConfigurationCustomizerProvider {
  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer
        .addTracerProviderCustomizer(
            (sdkTracerProviderBuilder, config) -> {
              addSpanProcessor(sdkTracerProviderBuilder, config);
              return sdkTracerProviderBuilder;
            })
        .addLoggerProviderCustomizer(
            (sdkLoggerProviderBuilder, config) -> {
              addLogRecordProcessor(sdkLoggerProviderBuilder, config);
              return sdkLoggerProviderBuilder;
            });
  }

  private static void addSpanProcessor(
      SdkTracerProviderBuilder sdkTracerProviderBuilder, ConfigProperties config) {
    List<String> keys =
        config.getList("otel.java.experimental.span-attributes.copy-from-baggage.include");

    if (keys.isEmpty()) {
      return;
    }

    // need to add before the batch span processor
    sdkTracerProviderBuilder.addSpanProcessorFirst(createBaggageSpanProcessor(keys));
  }

  static BaggageSpanProcessor createBaggageSpanProcessor(List<String> keys) {
    return new BaggageSpanProcessor(IncludeExcludePredicate.createPatternMatching(keys, null));
  }

  private static void addLogRecordProcessor(
      SdkLoggerProviderBuilder sdkLoggerProviderBuilder, ConfigProperties config) {
    List<String> keys =
        config.getList("otel.java.experimental.log-attributes.copy-from-baggage.include");

    if (keys.isEmpty()) {
      return;
    }

    // need to add before the batch log processor
    sdkLoggerProviderBuilder.addLogRecordProcessorFirst(createBaggageLogRecordProcessor(keys));
  }

  static BaggageLogRecordProcessor createBaggageLogRecordProcessor(List<String> keys) {
    return new BaggageLogRecordProcessor(IncludeExcludePredicate.createPatternMatching(keys, null));
  }
}
