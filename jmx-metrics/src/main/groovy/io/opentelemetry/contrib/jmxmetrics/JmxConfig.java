/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

class JmxConfig {
  static final String PREFIX = "otel.";
  static final String SERVICE_URL = PREFIX + "jmx.service.url";
  static final String GROOVY_SCRIPT = PREFIX + "jmx.groovy.script";
  static final String TARGET_SYSTEM = PREFIX + "jmx.target.system";
  static final String INTERVAL_MILLISECONDS = PREFIX + "jmx.interval.milliseconds";
  static final String METRICS_EXPORTER_TYPE = PREFIX + "metrics.exporter";
  static final String EXPORTER = PREFIX + "exporter.";
  static final String REGISTRY_SSL = PREFIX + "jmx.remote.registry.ssl";
  static final String EXPORTER_INTERVAL = PREFIX + "metric.export.interval";

  static final String OTLP_ENDPOINT = EXPORTER + "otlp.endpoint";

  static final String PROMETHEUS_HOST = EXPORTER + "prometheus.host";
  static final String PROMETHEUS_PORT = EXPORTER + "prometheus.port";

  static final String JMX_USERNAME = PREFIX + "jmx.username";
  static final String JMX_PASSWORD = PREFIX + "jmx.password";
  static final String JMX_REMOTE_PROFILE = PREFIX + "jmx.remote.profile";
  static final String JMX_REALM = PREFIX + "jmx.realm";
  static final String JMX_AGGREGATE_ACROSS_MBEANS = PREFIX + "jmx.aggregate.across.mbeans";

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

  final String serviceUrl;
  final String groovyScript;
  final String targetSystem;
  final Set<String> targetSystems;
  final int intervalMilliseconds;
  final String metricsExporterType;

  final String otlpExporterEndpoint;

  final String prometheusExporterHost;
  final int prometheusExporterPort;

  final String username;
  final String password;
  final String realm;
  final String remoteProfile;
  final boolean registrySsl;
  final Properties properties;

  final boolean aggregateAcrossMBeans;

  JmxConfig(final Properties props) {
    properties = new Properties();
    // putAll() instead of using constructor defaults
    // to ensure they will be recorded to underlying map
    properties.putAll(props);

    // command line takes precedence so replace any that were specified via config file properties
    properties.putAll(System.getProperties());

    serviceUrl = properties.getProperty(SERVICE_URL);
    groovyScript = properties.getProperty(GROOVY_SCRIPT);
    targetSystem = properties.getProperty(TARGET_SYSTEM, "").toLowerCase().trim();

    List<String> targets =
        Arrays.asList(isBlank(targetSystem) ? new String[0] : targetSystem.split(","));
    targetSystems = new LinkedHashSet<>(targets);

    int interval = getProperty(INTERVAL_MILLISECONDS, 10000);
    intervalMilliseconds = interval == 0 ? 10000 : interval;
    // set for autoconfigure usage
    getAndSetProperty(EXPORTER_INTERVAL, intervalMilliseconds);

    metricsExporterType = getAndSetProperty(METRICS_EXPORTER_TYPE, "logging");
    otlpExporterEndpoint = properties.getProperty(OTLP_ENDPOINT);

    prometheusExporterHost = getAndSetProperty(PROMETHEUS_HOST, "0.0.0.0");
    prometheusExporterPort = getAndSetProperty(PROMETHEUS_PORT, 9464);

    username = properties.getProperty(JMX_USERNAME);
    password = properties.getProperty(JMX_PASSWORD);

    remoteProfile = properties.getProperty(JMX_REMOTE_PROFILE);
    realm = properties.getProperty(JMX_REALM);

    registrySsl = Boolean.valueOf(properties.getProperty(REGISTRY_SSL));
    aggregateAcrossMBeans =
        Boolean.parseBoolean(properties.getProperty(JMX_AGGREGATE_ACROSS_MBEANS));

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

  JmxConfig() {
    this(new Properties());
  }

  private int getProperty(final String key, final int dfault) {
    String propVal = properties.getProperty(key);
    if (propVal == null) {
      return dfault;
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
  private String getAndSetProperty(final String key, final String defaultValue) {
    String propVal = properties.getProperty(key, defaultValue);
    if (propVal.equals(defaultValue)) {
      properties.setProperty(key, defaultValue);
    }
    return propVal;
  }

  private int getAndSetProperty(final String key, final int defaultValue) {
    int propVal = getProperty(key, defaultValue);
    if (propVal == defaultValue) {
      properties.setProperty(key, String.valueOf(defaultValue));
    }
    return propVal;
  }

  /** Will determine if parsed config is complete, setting any applicable values and defaults. */
  void validate() {
    if (isBlank(serviceUrl)) {
      throw new ConfigurationException(SERVICE_URL + " must be specified.");
    }

    if (isBlank(groovyScript) && isBlank(targetSystem)) {
      throw new ConfigurationException(
          GROOVY_SCRIPT + " or " + TARGET_SYSTEM + " must be specified.");
    }

    if (targetSystems.size() != 0 && !AVAILABLE_TARGET_SYSTEMS.containsAll(targetSystems)) {
      throw new ConfigurationException(
          String.format(
              "%s must specify targets from %s", targetSystems, AVAILABLE_TARGET_SYSTEMS));
    }

    if (isBlank(otlpExporterEndpoint)
        && (!isBlank(metricsExporterType) && metricsExporterType.equalsIgnoreCase("otlp"))) {
      throw new ConfigurationException(OTLP_ENDPOINT + " must be specified for otlp format.");
    }

    if (intervalMilliseconds < 0) {
      throw new ConfigurationException(INTERVAL_MILLISECONDS + " must be positive.");
    }
  }

  /**
   * Determines if a String is null or without non-whitespace chars.
   *
   * @param s - {@link String} to evaluate
   * @return - if s is null or without non-whitespace chars.
   */
  static boolean isBlank(final String s) {
    if (s == null) {
      return true;
    }
    return s.trim().length() == 0;
  }
}
