/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.baggage.processor;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.List;

@AutoService(AutoConfigurationCustomizerProvider.class)
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
    if (spanKeys(config).isEmpty()) {
      return;
    }

    sdkTracerProviderBuilder.addSpanProcessor(createBaggageSpanProcessor(config));
  }

  static BaggageSpanProcessor createBaggageSpanProcessor(ConfigProperties config) {
    return createBaggageSpanProcessor(spanKeys(config));
  }

  static BaggageSpanProcessor createBaggageSpanProcessor(List<String> keys) {
    if (matchAll(keys)) {
      return BaggageSpanProcessor.allowAllBaggageKeys();
    }
    return new BaggageSpanProcessor(keys::contains);
  }

  static List<String> spanKeys(ConfigProperties config) {
    return config.getList("otel.java.experimental.span-attributes.copy-from-baggage.include");
  }

  private static void addLogRecordProcessor(
      SdkLoggerProviderBuilder sdkLoggerProviderBuilder, ConfigProperties config) {
    if (logKeys(config).isEmpty()) {
      return;
    }

    sdkLoggerProviderBuilder.addLogRecordProcessor(createBaggageLogRecordProcessor(config));
  }

  static BaggageLogRecordProcessor createBaggageLogRecordProcessor(ConfigProperties config) {
    return createBaggageLogRecordProcessor(logKeys(config));
  }

  static BaggageLogRecordProcessor createBaggageLogRecordProcessor(List<String> keys) {
    if (matchAll(keys)) {
      return BaggageLogRecordProcessor.allowAllBaggageKeys();
    }
    return new BaggageLogRecordProcessor(keys::contains);
  }

  static List<String> logKeys(ConfigProperties config) {
    return config.getList("otel.java.experimental.log-attributes.copy-from-baggage.include");
  }

  private static boolean matchAll(List<String> keys) {
    return keys.size() == 1 && keys.get(0).equals("*");
  }
}
