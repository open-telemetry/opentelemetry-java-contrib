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

import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_INTERVAL_LEGACY;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.METRIC_EXPORT_INTERVAL;

/** Customizer of default SDK configuration and provider of effective scraper config */
public class PropertiesCustomizer implements Function<ConfigProperties, Map<String, String>> {

  private static final String METRICS_EXPORTER = "otel.metrics.exporter";

  @Nullable private JmxScraperConfig scraperConfig;

  @Override
  public Map<String, String> apply(ConfigProperties config) {
    Map<String, String> result = new HashMap<>();

    // set default exporter to logging when not explicitly set
    if (config.getList(METRICS_EXPORTER).isEmpty()) {
      // TODO: log (info) this
      result.put(METRICS_EXPORTER, "logging");
    }

    // providing compatibility with the existing 'otel.jmx.interval.milliseconds' config option
    long intervalLegacy = config.getLong(JMX_INTERVAL_LEGACY, -1);
    if (config.getDuration(METRIC_EXPORT_INTERVAL) == null && intervalLegacy > 0) {
      // TODO: log (warn) this
      result.put(METRIC_EXPORT_INTERVAL, intervalLegacy + "ms");
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
