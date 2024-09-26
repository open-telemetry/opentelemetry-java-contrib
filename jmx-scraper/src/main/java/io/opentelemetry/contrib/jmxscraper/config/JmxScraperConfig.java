/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static io.opentelemetry.contrib.jmxscraper.internal.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** This class keeps application settings */
public class JmxScraperConfig {

  static final String SERVICE_URL = "otel.jmx.service.url";
  static final String CUSTOM_JMX_SCRAPING_CONFIG = "otel.jmx.custom.scraping.config";
  static final String TARGET_SYSTEM = "otel.jmx.target.system";
  static final String INTERVAL_MILLISECONDS = "otel.jmx.interval.milliseconds";
  static final String METRICS_EXPORTER_TYPE = "otel.metrics.exporter";
  static final String EXPORTER_INTERVAL = "otel.metric.export.interval";
  static final String REGISTRY_SSL = "otel.jmx.remote.registry.ssl";

  static final String OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";

  static final String JMX_USERNAME = "otel.jmx.username";
  static final String JMX_PASSWORD = "otel.jmx.password";
  static final String JMX_REMOTE_PROFILE = "otel.jmx.remote.profile";
  static final String JMX_REALM = "otel.jmx.realm";

  static final List<String> AVAILABLE_TARGET_SYSTEMS =
      Collections.unmodifiableList(
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
              "wildfly"));

  private String serviceUrl = "";
  private String customJmxScrapingConfigPath = "";
  private Set<String> targetSystems = Collections.emptySet();
  private int intervalMilliseconds;
  private String metricsExporterType = "";
  private String otlpExporterEndpoint = "";
  private String username = "";
  private String password = "";
  private String realm = "";
  private String remoteProfile = "";
  private boolean registrySsl;

  /** Combined properties kept for initializing system properties */
  private final Properties properties;

  private JmxScraperConfig(Properties properties) {
    this.properties = properties;
  }

  public String getServiceUrl() {
    return serviceUrl;
  }

  public String getCustomJmxScrapingConfigPath() {
    return customJmxScrapingConfigPath;
  }

  public Set<String> getTargetSystems() {
    return targetSystems;
  }

  public int getIntervalMilliseconds() {
    return intervalMilliseconds;
  }

  public String getMetricsExporterType() {
    return metricsExporterType;
  }

  public String getOtlpExporterEndpoint() {
    return otlpExporterEndpoint;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getRealm() {
    return realm;
  }

  public String getRemoteProfile() {
    return remoteProfile;
  }

  public boolean isRegistrySsl() {
    return registrySsl;
  }

  /**
   * Builds scraper configuration from user and system properties
   *
   * @param userProperties user-provided configuration
   * @param systemProperties system properties through '-Dxxx' JVM arguments
   * @return JMX scraper configuration
   * @throws ConfigurationException if there is any configuration error
   */
  public static JmxScraperConfig fromProperties(
      Properties userProperties, Properties systemProperties) throws ConfigurationException {

    Properties properties = new Properties();
    properties.putAll(userProperties);

    // command line takes precedence so replace any that were specified via config file properties
    properties.putAll(systemProperties);

    JmxScraperConfig config = new JmxScraperConfig(properties);

    config.serviceUrl = properties.getProperty(SERVICE_URL);
    config.customJmxScrapingConfigPath = properties.getProperty(CUSTOM_JMX_SCRAPING_CONFIG);
    String targetSystem =
        properties.getProperty(TARGET_SYSTEM, "").toLowerCase(Locale.ENGLISH).trim();

    List<String> targets =
        Arrays.asList(isBlank(targetSystem) ? new String[0] : targetSystem.split(","));
    config.targetSystems = targets.stream().map(String::trim).collect(Collectors.toSet());

    int interval = getProperty(properties, INTERVAL_MILLISECONDS, 0);
    config.intervalMilliseconds = (interval == 0 ? 10000 : interval);
    // configure SDK metric exporter interval from jmx metric interval
    getAndSetPropertyIfUndefined(properties, EXPORTER_INTERVAL, config.intervalMilliseconds);

    config.metricsExporterType =
        getAndSetPropertyIfUndefined(properties, METRICS_EXPORTER_TYPE, "logging");
    config.otlpExporterEndpoint = properties.getProperty(OTLP_ENDPOINT);

    config.username = properties.getProperty(JMX_USERNAME);
    config.password = properties.getProperty(JMX_PASSWORD);

    config.remoteProfile = properties.getProperty(JMX_REMOTE_PROFILE);
    config.realm = properties.getProperty(JMX_REALM);

    config.registrySsl = Boolean.parseBoolean(properties.getProperty(REGISTRY_SSL));

    validateConfig(config);
    return config;
  }

  /**
   * Sets system properties from effective configuration, must be called once and early before any
   * OTel SDK or SSL/TLS stack initialization. This allows to override JVM system properties using
   * user-provided configuration and also to set standard OTel SDK configuration.
   */
  public void propagateSystemProperties() {
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {

      String key = entry.getKey().toString();
      String value = entry.getValue().toString();
      if (key.startsWith("otel.")
          || key.startsWith("javax.net.ssl.keyStore")
          || key.startsWith("javax.net.ssl.trustStore")) {
        System.setProperty(key, value);
      }
    }
  }

  private static int getProperty(Properties properties, String key, int defaultValue)
      throws ConfigurationException {
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
  private static String getAndSetPropertyIfUndefined(
      Properties properties, String key, String defaultValue) {
    String propVal = properties.getProperty(key, defaultValue);
    if (propVal.equals(defaultValue)) {
      properties.setProperty(key, defaultValue);
    }
    return propVal;
  }

  private static int getAndSetPropertyIfUndefined(
      Properties properties, String key, int defaultValue) throws ConfigurationException {
    int propVal = getProperty(properties, key, defaultValue);
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
