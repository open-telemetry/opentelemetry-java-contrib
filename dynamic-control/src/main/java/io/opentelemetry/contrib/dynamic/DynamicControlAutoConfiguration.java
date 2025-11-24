/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoConfigurationCustomizerProvider for dynamic control extension.
 *
 * <p>This extension provides a skeleton for dynamic control of agent features. Currently, it just
 * logs when loaded by the agent.
 */
@AutoService(AutoConfigurationCustomizerProvider.class)
public class DynamicControlAutoConfiguration implements AutoConfigurationCustomizerProvider {

  private static final Logger logger =
      Logger.getLogger(DynamicControlAutoConfiguration.class.getName());

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    logger.log(Level.INFO, "Dynamic control extension has been loaded by the agent");
  }

  @Override
  public int order() {
    return 0;
  }
}
