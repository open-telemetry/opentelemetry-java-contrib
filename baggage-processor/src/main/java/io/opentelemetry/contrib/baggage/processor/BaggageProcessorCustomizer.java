/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.List;

public class BaggageProcessorCustomizer implements AutoConfigurationCustomizerProvider {
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
    if (keys.size() == 1 && keys.get(0).equals("*")) {
      return BaggageSpanProcessor.allowAllBaggageKeys();
    }
    return new BaggageSpanProcessor(keys::contains);
  }

  private static void addLogRecordProcessor(
      SdkLoggerProviderBuilder sdkLoggerProviderBuilder, ConfigProperties config) {
    List<String> keys =
        config.getList("otel.java.experimental.log-attributes.copy-from-baggage.include");

    if (keys.isEmpty()) {
      return;
    }

    // need to add before the batch span processor
    sdkLoggerProviderBuilder.addLogRecordProcessorFirst(createBaggageLogRecordProcessor(keys));
  }

  static BaggageLogRecordProcessor createBaggageLogRecordProcessor(List<String> keys) {
    if (keys.size() == 1 && keys.get(0).equals("*")) {
      return BaggageLogRecordProcessor.allowAllBaggageKeys();
    }
    return new BaggageLogRecordProcessor(keys::contains);
  }
}
