/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static io.opentelemetry.contrib.inferredspans.InferredSpansConfig.ENABLED_OPTION;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansAutoConfig.class.getName());

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          if (properties.getBoolean(ENABLED_OPTION, false)) {
            providerBuilder.addSpanProcessor(InferredSpansConfig.createSpanProcessor(properties));
          } else {
            log.finest(
                "Not enabling inferred spans processor because " + ENABLED_OPTION + " is not set");
          }
          return providerBuilder;
        });
  }
}
