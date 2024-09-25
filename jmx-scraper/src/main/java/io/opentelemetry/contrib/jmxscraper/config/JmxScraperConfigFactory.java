/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static io.opentelemetry.contrib.jmxscraper.util.StringUtils.isBlank;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public class JmxScraperConfigFactory {
  private static final String PREFIX = "otel.";
  static final String SERVICE_URL = PREFIX + "jmx.service.url";
  static final String CUSTOM_JMX_SCRAPING_CONFIG = PREFIX + "jmx.custom.scraping.config";
  static final String TARGET_SYSTEM = PREFIX + "jmx.target.system";
  static final String INTERVAL_MILLISECONDS = PREFIX + "jmx.interval.milliseconds";
  static final String METRICS_EXPORTER_TYPE = PREFIX + "metrics.exporter";
  static final String EXPORTER_INTERVAL = PREFIX + "metric.export.interval";
  static final String REGISTRY_SSL = PREFIX + "jmx.remote.registry.ssl";

  static final String OTLP_ENDPOINT = PREFIX + "exporter.otlp.endpoint";

  static final String JMX_USERNAME = PREFIX + "jmx.username";
  static final String JMX_PASSWORD = PREFIX + "jmx.password";
  static final String JMX_REMOTE_PROFILE = PREFIX + "jmx.remote.profile";
  static final String JMX_REALM = PREFIX + "jmx.realm";

  // These properties need to be copied into System Properties if provided via the property
  // file so that they are available to the JMX Connection builder
  static final List<String> JAVA_SYSTEM_PROPERTIES =
      Arrays.asList(
          "javax.net.ssl.keyStore",
          "javax.net.ssl.keyStorePassword",
          "javax.net.ssl.keyStoreType",
          "javax.net.ssl.trustStore",
          "javax.net.ssl.trustStorePassword",
          "javax.net.ssl.trustStoreType");

  static final List<String> AVAILABLE_TARGET_SYSTEMS =
      Arrays.asList(
          "activemq",
          "cassandra",
          "hbase",
          "hadoop",
          "jetty",
          "jvm",
          "kafka",
          "kafka-consumer",
          "kafka-producer",
          "solr",
          "tomcat",
          "wildfly");

  private Properties properties = new Properties();

  public JmxScraperConfig createConfig(Properties props) throws ConfigurationException {
    properties = new Properties();
    // putAll() instead of using constructor defaults
    // to ensure they will be recorded to underlying map
    properties.putAll(props);

    // command line takes precedence so replace any that were specified via config file properties
    properties.putAll(System.getProperties());

    JmxScraperConfig config = new JmxScraperConfig();

    config.serviceUrl = properties.getProperty(SERVICE_URL);
    config.customJmxScrapingConfigPath = properties.getProperty(CUSTOM_JMX_SCRAPING_CONFIG);
    String targetSystem =
        properties.getProperty(TARGET_SYSTEM, "").toLowerCase(Locale.ENGLISH).trim();

    List<String> targets =
        Arrays.asList(isBlank(targetSystem) ? new String[0] : targetSystem.split(","));
    config.targetSystems = targets.stream().map(String::trim).collect(Collectors.toSet());

    int interval = getProperty(INTERVAL_MILLISECONDS, 0);
    config.intervalMilliseconds = (interval == 0 ? 10000 : interval);
    getAndSetPropertyIfUndefined(EXPORTER_INTERVAL, config.intervalMilliseconds);

    config.metricsExporterType = getAndSetPropertyIfUndefined(METRICS_EXPORTER_TYPE, "logging");
    config.otlpExporterEndpoint = properties.getProperty(OTLP_ENDPOINT);

    config.username = properties.getProperty(JMX_USERNAME);
    config.password = properties.getProperty(JMX_PASSWORD);

    config.remoteProfile = properties.getProperty(JMX_REMOTE_PROFILE);
    config.realm = properties.getProperty(JMX_REALM);

    config.registrySsl = Boolean.parseBoolean(properties.getProperty(REGISTRY_SSL));

    validateConfig(config);
    populateJmxSystemProperties();

    return config;
  }

  private void populateJmxSystemProperties() {
    // For the list of System Properties, if they have been set in the properties file
    // they need to be set in Java System Properties.
    JAVA_SYSTEM_PROPERTIES.forEach(
        key -> {
          // As properties file & command line properties are combined into properties
          // at this point, only override if it was not already set via command line
          if (System.getProperty(key) != null) {
            return;
          }
          String value = properties.getProperty(key);
          if (value != null) {
            System.setProperty(key, value);
          }
        });
  }

  private int getProperty(String key, int defaultValue) throws ConfigurationException {
    String propVal = properties.getProperty(key);
    if (propVal == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(propVal);
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Failed to parse " + key, e);
    }
  }

  /**
   * Similar to getProperty(key, defaultValue) but sets the property to default if not in object.
   */
  private String getAndSetPropertyIfUndefined(String key, String defaultValue) {
    String propVal = properties.getProperty(key, defaultValue);
    if (propVal.equals(defaultValue)) {
      properties.setProperty(key, defaultValue);
    }
    return propVal;
  }

  private int getAndSetPropertyIfUndefined(String key, int defaultValue)
      throws ConfigurationException {
    int propVal = getProperty(key, defaultValue);
    if (propVal == defaultValue) {
      properties.setProperty(key, String.valueOf(defaultValue));
    }
    return propVal;
  }

  /** Will determine if parsed config is complete, setting any applicable values and defaults. */
  private static void validateConfig(JmxScraperConfig config) throws ConfigurationException {
    if (isBlank(config.serviceUrl)) {
      throw new ConfigurationException(SERVICE_URL + " must be specified.");
    }

    if (isBlank(config.customJmxScrapingConfigPath) && config.targetSystems.isEmpty()) {
      throw new ConfigurationException(
          CUSTOM_JMX_SCRAPING_CONFIG + " or " + TARGET_SYSTEM + " must be specified.");
    }

    if (!config.targetSystems.isEmpty()
        && !AVAILABLE_TARGET_SYSTEMS.containsAll(config.targetSystems)) {
      throw new ConfigurationException(
          String.format(
              "%s must specify targets from %s", config.targetSystems, AVAILABLE_TARGET_SYSTEMS));
    }

    if (isBlank(config.otlpExporterEndpoint)
        && (!isBlank(config.metricsExporterType)
            && config.metricsExporterType.equalsIgnoreCase("otlp"))) {
      throw new ConfigurationException(OTLP_ENDPOINT + " must be specified for otlp format.");
    }

    if (config.intervalMilliseconds < 0) {
      throw new ConfigurationException(INTERVAL_MILLISECONDS + " must be positive.");
    }
  }
}
