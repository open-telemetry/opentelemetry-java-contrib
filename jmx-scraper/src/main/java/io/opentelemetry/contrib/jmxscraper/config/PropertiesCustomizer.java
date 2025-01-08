/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Customizer of default SDK configuration and provider of effective scraper config */
public class PropertiesCustomizer implements Function<ConfigProperties, Map<String, String>> {

  private static final String METRICS_EXPORTER = "otel.metrics.exporter";

  @Nullable private JmxScraperConfig scraperConfig;

  @Override
  public Map<String, String> apply(ConfigProperties config) {
    Map<String, String> result = new HashMap<>();
    if (config.getList(METRICS_EXPORTER).isEmpty()) {
      // default exporter to logging when not explicitly set
      // TODO: log this
      result.put(METRICS_EXPORTER, "logging");
    }

    scraperConfig = JmxScraperConfig.fromConfig(config);
    return result;
  }

  /**
   * Get scraper configuration from the previous call to {@link #apply(ConfigProperties)}
   *
   * @return JMX scraper configuration
   * @throws IllegalStateException when {@link #apply(ConfigProperties)} hasn't been called first
   */
  public JmxScraperConfig getScraperConfig() {
    if (scraperConfig == null) {
      throw new IllegalStateException("apply() must be called before getConfig()");
    }
    return scraperConfig;
  }
}
