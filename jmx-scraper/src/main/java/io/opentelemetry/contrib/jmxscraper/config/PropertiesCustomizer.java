/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Customizer of default SDK configuration and provider of effective scraper config */
public class PropertiesCustomizer implements Function<ConfigProperties, Map<String, String>> {

  @Nullable private JmxScraperConfig scraperConfig;

  @Override
  public Map<String, String> apply(ConfigProperties config) {
    return Collections.emptyMap();
  }

  /**
   * @return JMX scraper configuration
   * @throws ConfigurationException when config is invalid
   * @throws IllegalStateException when {@link #apply(ConfigProperties)} hasn't been called first
   */
  public JmxScraperConfig getScraperConfig() throws ConfigurationException {
    if (scraperConfig == null) {
      throw new IllegalStateException("apply() must be called before getConfig()");
    }
    return null;
  }
}
