/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_INTERVAL_LEGACY;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.METRIC_EXPORT_INTERVAL;

import io.opentelemetry.contrib.jmxscraper.JmxConnectorBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Customizer of default SDK configuration and provider of effective scraper config */
public class PropertiesCustomizer implements Function<ConfigProperties, Map<String, String>> {

  private static final Logger logger = Logger.getLogger(PropertiesCustomizer.class.getName());

  private static final String METRICS_EXPORTER = "otel.metrics.exporter";

  @Nullable private JmxScraperConfig scraperConfig;

  @Nullable private JmxConnectorBuilder connectorBuilder;

  @Override
  public Map<String, String> apply(ConfigProperties config) {
    Map<String, String> result = new HashMap<>();

    // set default exporter to 'otlp' to be consistent with SDK
    if (config.getList(METRICS_EXPORTER).isEmpty()) {
      result.put(METRICS_EXPORTER, "otlp");
    }

    // providing compatibility with the existing 'otel.jmx.interval.milliseconds' config option
    long intervalLegacy = config.getLong(JMX_INTERVAL_LEGACY, -1);
    if (config.getDuration(METRIC_EXPORT_INTERVAL) == null && intervalLegacy >= 0) {
      logger.warning(
          METRIC_EXPORT_INTERVAL
              + " deprecated option is used, replacing with '"
              + METRIC_EXPORT_INTERVAL
              + "' metric sdk configuration is recommended");
      result.put(METRIC_EXPORT_INTERVAL, intervalLegacy + "ms");
    }

    scraperConfig = JmxScraperConfig.fromConfig(config);

    long exportSeconds = scraperConfig.getSamplingInterval().toMillis() / 1000;
    logger.log(Level.INFO, "metrics export interval (seconds) =  " + exportSeconds);

    connectorBuilder = JmxConnectorBuilder.createNew(scraperConfig.getServiceUrl());

    Optional.ofNullable(scraperConfig.getUsername()).ifPresent(connectorBuilder::withUser);
    Optional.ofNullable(scraperConfig.getPassword()).ifPresent(connectorBuilder::withPassword);

    if (scraperConfig.isRegistrySsl()) {
      connectorBuilder.withSslRegistry();
    }

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

  public JmxConnectorBuilder getConnectorBuilder() {
    if (connectorBuilder == null) {
      throw new IllegalStateException("apply() must be called before getConnectorBuilder()");
    }
    return connectorBuilder;
  }
}
