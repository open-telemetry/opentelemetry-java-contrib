/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.CUSTOM_JMX_SCRAPING_CONFIG;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.INTERVAL_MILLISECONDS;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_PASSWORD;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_REALM;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_REMOTE_PROFILE;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_USERNAME;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.METRICS_EXPORTER_TYPE;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.OTLP_ENDPOINT;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.REGISTRY_SSL;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.SERVICE_URL;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.TARGET_SYSTEM;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.fromProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

class JmxScraperConfigTest {
  private static Properties validProperties;

  @BeforeAll
  static void setUp() {
    validProperties = new Properties();
    validProperties.setProperty(
        SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    validProperties.setProperty(CUSTOM_JMX_SCRAPING_CONFIG, "");
    validProperties.setProperty(TARGET_SYSTEM, "tomcat, activemq");
    validProperties.setProperty(METRICS_EXPORTER_TYPE, "otel");
    validProperties.setProperty(INTERVAL_MILLISECONDS, "1410");
    validProperties.setProperty(REGISTRY_SSL, "true");
    validProperties.setProperty(OTLP_ENDPOINT, "http://localhost:4317");
    validProperties.setProperty(JMX_USERNAME, "some-user");
    validProperties.setProperty(JMX_PASSWORD, "some-password");
    validProperties.setProperty(JMX_REMOTE_PROFILE, "some-profile");
    validProperties.setProperty(JMX_REALM, "some-realm");
  }

  @AfterEach
  void afterEach() {
    // make sure that no test leaked in global system properties
    Stream.of(System.getProperties().keySet())
        .map(Object::toString)
        .forEach(
            key -> {
              if (key.startsWith("otel.") || key.startsWith("javax.net.ssl.")) {
                System.clearProperty(key);
              }
            });
  }

  @Test
  void shouldCreateMinimalValidConfiguration() throws ConfigurationException {
    // Given
    Properties properties = new Properties();
    properties.setProperty(SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    properties.setProperty(CUSTOM_JMX_SCRAPING_CONFIG, "/file.properties");

    // When
    JmxScraperConfig config = fromProperties(properties, new Properties());

    // Then
    assertThat(config.getServiceUrl())
        .isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    assertThat(config.getCustomJmxScrapingConfigPath()).isEqualTo("/file.properties");
    assertThat(config.getTargetSystems()).isEmpty();
    assertThat(config.getIntervalMilliseconds()).isEqualTo(10000);
    assertThat(config.getMetricsExporterType()).isEqualTo("logging");
    assertThat(config.getOtlpExporterEndpoint()).isNull();
    assertThat(config.getUsername()).isNull();
    assertThat(config.getPassword()).isNull();
    assertThat(config.getRemoteProfile()).isNull();
    assertThat(config.getRealm()).isNull();
  }

  @Test
  @ClearSystemProperty(key = "javax.net.ssl.keyStore")
  @ClearSystemProperty(key = "javax.net.ssl.keyStorePassword")
  @ClearSystemProperty(key = "javax.net.ssl.keyStoreType")
  @ClearSystemProperty(key = "javax.net.ssl.trustStore")
  @ClearSystemProperty(key = "javax.net.ssl.trustStorePassword")
  @ClearSystemProperty(key = "javax.net.ssl.trustStoreType")
  void shouldUseValuesFromProperties() throws ConfigurationException {
    // Given
    // Properties to be propagated to system, properties
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty("javax.net.ssl.keyStore", "/my/key/store");
    properties.setProperty("javax.net.ssl.keyStorePassword", "abc123");
    properties.setProperty("javax.net.ssl.keyStoreType", "JKS");
    properties.setProperty("javax.net.ssl.trustStore", "/my/trust/store");
    properties.setProperty("javax.net.ssl.trustStorePassword", "def456");
    properties.setProperty("javax.net.ssl.trustStoreType", "JKS");

    assertThat(System.getProperty("javax.net.ssl.keyStore"))
        .describedAs("keystore config should not be set")
        .isNull();

    // When
    JmxScraperConfig config = fromProperties(properties, new Properties());
    config.propagateSystemProperties();

    // Then
    assertThat(config.getServiceUrl())
        .isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    assertThat(config.getCustomJmxScrapingConfigPath()).isEqualTo("");
    assertThat(config.getTargetSystems()).containsOnly("tomcat", "activemq");
    assertThat(config.getIntervalMilliseconds()).isEqualTo(1410);
    assertThat(config.getMetricsExporterType()).isEqualTo("otel");
    assertThat(config.getOtlpExporterEndpoint()).isEqualTo("http://localhost:4317");
    assertThat(config.getUsername()).isEqualTo("some-user");
    assertThat(config.getPassword()).isEqualTo("some-password");
    assertThat(config.getRemoteProfile()).isEqualTo("some-profile");
    assertThat(config.getRealm()).isEqualTo("some-realm");
    assertThat(config.isRegistrySsl()).isTrue();

    // These properties are set from the config file loading into JmxConfig
    assertThat(System.getProperty("javax.net.ssl.keyStore")).isEqualTo("/my/key/store");
    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("abc123");
    assertThat(System.getProperty("javax.net.ssl.keyStoreType")).isEqualTo("JKS");
    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo("/my/trust/store");
    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo("def456");
    assertThat(System.getProperty("javax.net.ssl.trustStoreType")).isEqualTo("JKS");
  }

  @Test
  @ClearSystemProperty(key = "otel.jmx.service.url")
  @ClearSystemProperty(key = "javax.net.ssl.keyStorePassword")
  void shouldRetainPredefinedSystemProperties() throws ConfigurationException {
    // Given
    // user properties to be propagated to system properties
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty("javax.net.ssl.keyStorePassword", "abc123");

    // system properties
    Properties systemProperties = new Properties();
    systemProperties.put("otel.jmx.service.url", "originalServiceUrl");
    systemProperties.put("javax.net.ssl.keyStorePassword", "originalPassword");

    // When
    JmxScraperConfig config = fromProperties(properties, systemProperties);
    // even when effective configuration is propagated to system properties original values are kept
    // due to priority of system properties over user-provided ones.
    config.propagateSystemProperties();

    // Then
    assertThat(System.getProperty("otel.jmx.service.url")).isEqualTo("originalServiceUrl");
    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("originalPassword");
  }

  @Test
  void shouldFailValidation_missingServiceUrl() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(SERVICE_URL);

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.service.url must be specified.");
  }

  @Test
  void shouldFailValidation_missingConfigPathAndTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(CUSTOM_JMX_SCRAPING_CONFIG);
    properties.remove(TARGET_SYSTEM);

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.custom.scraping.config or otel.jmx.target.system must be specified.");
  }

  @Test
  void shouldFailValidation_invalidTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(TARGET_SYSTEM, "hal9000");

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageStartingWith("[hal9000] must specify targets from ");
  }

  @Test
  void shouldFailValidation_missingOtlpEndpoint() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(OTLP_ENDPOINT);
    properties.setProperty(METRICS_EXPORTER_TYPE, "otlp");

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.exporter.otlp.endpoint must be specified for otlp format.");
  }

  @Test
  void shouldPassValidation_noMetricsExporterType() throws ConfigurationException {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(OTLP_ENDPOINT);
    properties.remove(METRICS_EXPORTER_TYPE);

    // When
    JmxScraperConfig config = fromProperties(properties, new Properties());

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldPassValidation_nonOtlpMetricsExporterType() throws ConfigurationException {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(OTLP_ENDPOINT);
    properties.setProperty(METRICS_EXPORTER_TYPE, "logging");

    // When
    JmxScraperConfig config = fromProperties(properties, new Properties());

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldFailValidation_negativeInterval() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(INTERVAL_MILLISECONDS, "-1");

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.interval.milliseconds must be positive.");
  }

  @Test
  void shouldFailConfigCreation_invalidInterval() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(INTERVAL_MILLISECONDS, "abc");

    // When and Then
    assertThatThrownBy(() -> fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("Failed to parse otel.jmx.interval.milliseconds");
  }

  // TODO: Tests below will be reimplemented

  //  @Test
  //  @SetSystemProperty(key = "otel.jmx.service.url", value = "myServiceUrl")
  //  @SetSystemProperty(key = "javax.net.ssl.keyStorePassword", value = "truth")
  //  void propertiesFileOverride() {
  //    Properties props = new Properties();
  //    JmxMetrics.loadPropertiesFromPath(
  //        props, ClassLoader.getSystemClassLoader().getResource("all.properties").getPath());
  //    JmxConfig config = new JmxConfig(props);
  //
  //    // This property should retain the system property value, not the config file value
  //    assertThat(config.serviceUrl).isEqualTo("myServiceUrl");
  //    // These properties are set from the config file
  //    assertThat(config.groovyScript).isEqualTo("/my/groovy/script");
  //    assertThat(config.targetSystem).isEqualTo("jvm,cassandra");
  //    assertThat(config.targetSystems).containsOnly("jvm", "cassandra");
  //    assertThat(config.intervalMilliseconds).isEqualTo(20000);
  //    assertThat(config.metricsExporterType).isEqualTo("otlp");
  //    assertThat(config.otlpExporterEndpoint).isEqualTo("https://myotlpendpoint");
  //    assertThat(config.prometheusExporterHost).isEqualTo("host123.domain.com");
  //    assertThat(config.prometheusExporterPort).isEqualTo(67890);
  //    assertThat(config.username).isEqualTo("myUser\nname");
  //    assertThat(config.password).isEqualTo("myPassw\\ord");
  //    assertThat(config.remoteProfile).isEqualTo("SASL/DIGEST-MD5");
  //    assertThat(config.realm).isEqualTo("myRealm");
  //
  //    // This property should retain the system property value, not the config file value
  //    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("truth");
  //    // These properties are set from the config file loading into JmxConfig
  //    assertThat(System.getProperty("javax.net.ssl.keyStore")).isEqualTo("/my/key/store");
  //    assertThat(System.getProperty("javax.net.ssl.keyStoreType")).isEqualTo("JKS");
  //    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo("/my/trust/store");
  //    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo("def456");
  //    assertThat(System.getProperty("javax.net.ssl.trustStoreType")).isEqualTo("JKS");
  //  }
  //
  //  @Test
  //  @SetSystemProperty(key = "otel.jmx.service.url", value = "myServiceUrl")
  //  @SetSystemProperty(key = "otel.jmx.groovy.script", value = "myGroovyScript")
  //  @SetSystemProperty(key = "otel.jmx.target.system", value = "myTargetSystem")
  //  void canSupportScriptAndTargetSystem() {
  //    JmxConfig config = new JmxConfig();
  //
  //    assertThat(config.serviceUrl).isEqualTo("myServiceUrl");
  //    assertThat(config.groovyScript).isEqualTo("myGroovyScript");
  //    assertThat(config.targetSystem).isEqualTo("mytargetsystem");
  //    assertThat(config.targetSystems).containsOnly("mytargetsystem");
  //  }
  //
  //  @Test
  //  @SetSystemProperty(key = "otel.metric.export.interval", value = "123")
  //  void otelMetricExportIntervalRespected() {
  //    JmxConfig config = new JmxConfig();
  //    assertThat(config.intervalMilliseconds).isEqualTo(10000);
  //    assertThat(config.properties.getProperty("otel.metric.export.interval")).isEqualTo("123");
  //  }
  //
}
