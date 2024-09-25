/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

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
        JmxScraperConfig.SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    validProperties.setProperty(JmxScraperConfig.CUSTOM_JMX_SCRAPING_CONFIG, "");
    validProperties.setProperty(JmxScraperConfig.TARGET_SYSTEM, "tomcat, activemq");
    validProperties.setProperty(JmxScraperConfig.METRICS_EXPORTER_TYPE, "otel");
    validProperties.setProperty(JmxScraperConfig.INTERVAL_MILLISECONDS, "1410");
    validProperties.setProperty(JmxScraperConfig.REGISTRY_SSL, "true");
    validProperties.setProperty(JmxScraperConfig.OTLP_ENDPOINT, "http://localhost:4317");
    validProperties.setProperty(JmxScraperConfig.JMX_USERNAME, "some-user");
    validProperties.setProperty(JmxScraperConfig.JMX_PASSWORD, "some-password");
    validProperties.setProperty(JmxScraperConfig.JMX_REMOTE_PROFILE, "some-profile");
    validProperties.setProperty(JmxScraperConfig.JMX_REALM, "some-realm");
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
    properties.setProperty(
        JmxScraperConfig.SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    properties.setProperty(JmxScraperConfig.CUSTOM_JMX_SCRAPING_CONFIG, "/file.properties");

    // When
    JmxScraperConfig config = JmxScraperConfig.fromProperties(properties, new Properties());

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
    JmxScraperConfig config = JmxScraperConfig.fromProperties(properties, new Properties());
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
    JmxScraperConfig config = JmxScraperConfig.fromProperties(properties, systemProperties);
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
    properties.remove(JmxScraperConfig.SERVICE_URL);

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.service.url must be specified.");
  }

  @Test
  void shouldFailValidation_missingConfigPathAndTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfig.CUSTOM_JMX_SCRAPING_CONFIG);
    properties.remove(JmxScraperConfig.TARGET_SYSTEM);

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.custom.scraping.config or otel.jmx.target.system must be specified.");
  }

  @Test
  void shouldFailValidation_invalidTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfig.TARGET_SYSTEM, "hal9000");

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageStartingWith("[hal9000] must specify targets from ");
  }

  @Test
  void shouldFailValidation_missingOtlpEndpoint() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfig.OTLP_ENDPOINT);
    properties.setProperty(JmxScraperConfig.METRICS_EXPORTER_TYPE, "otlp");

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.exporter.otlp.endpoint must be specified for otlp format.");
  }

  @Test
  void shouldPassValidation_noMetricsExporterType() throws ConfigurationException {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfig.OTLP_ENDPOINT);
    properties.remove(JmxScraperConfig.METRICS_EXPORTER_TYPE);

    // When
    JmxScraperConfig config = JmxScraperConfig.fromProperties(properties, new Properties());

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldPassValidation_nonOtlpMetricsExporterType() throws ConfigurationException {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfig.OTLP_ENDPOINT);
    properties.setProperty(JmxScraperConfig.METRICS_EXPORTER_TYPE, "logging");

    // When
    JmxScraperConfig config = JmxScraperConfig.fromProperties(properties, new Properties());

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldFailValidation_negativeInterval() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfig.INTERVAL_MILLISECONDS, "-1");

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.interval.milliseconds must be positive.");
  }

  @Test
  void shouldFailConfigCreation_invalidInterval() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfig.INTERVAL_MILLISECONDS, "abc");

    // When and Then
    assertThatThrownBy(() -> JmxScraperConfig.fromProperties(properties, new Properties()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("Failed to parse otel.jmx.interval.milliseconds");
  }

  //  @ClearSystemProperty(key = "otel.metric.export.interval")

  //  @Test
  //  @SetSystemProperty(key = "otel.jmx.service.url", value = "myServiceUrl")
  //  @SetSystemProperty(key = "otel.jmx.groovy.script", value = "myGroovyScript")
  //  @SetSystemProperty(
  //      key = "otel.jmx.target.system",
  //      value = "mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem")
  //  @SetSystemProperty(key = "otel.jmx.interval.milliseconds", value = "123")
  //  @SetSystemProperty(key = "otel.metrics.exporter", value = "inmemory")
  //  @SetSystemProperty(key = "otel.exporter.otlp.endpoint", value = "https://myOtlpEndpoint")
  //  @SetSystemProperty(key = "otel.exporter.prometheus.host", value = "myPrometheusHost")
  //  @SetSystemProperty(key = "otel.exporter.prometheus.port", value = "234")
  //  @SetSystemProperty(key = "otel.jmx.username", value = "myUsername")
  //  @SetSystemProperty(key = "otel.jmx.password", value = "myPassword")
  //  @SetSystemProperty(key = "otel.jmx.remote.profile", value = "myRemoteProfile")
  //  @SetSystemProperty(key = "otel.jmx.realm", value = "myRealm")
  //  void specifiedValues() {
  //    JmxConfig config = new JmxConfig();
  //
  //    assertThat(config.serviceUrl).isEqualTo("myServiceUrl");
  //    assertThat(config.groovyScript).isEqualTo("myGroovyScript");
  //    assertThat(config.targetSystem)
  //
  // .isEqualTo("mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem");
  //    assertThat(config.targetSystems)
  //        .containsOnly("mytargetsystem", "myothertargetsystem", "myadditionaltargetsystem");
  //    assertThat(config.intervalMilliseconds).isEqualTo(123);
  //    assertThat(config.metricsExporterType).isEqualTo("inmemory");
  //    assertThat(config.otlpExporterEndpoint).isEqualTo("https://myOtlpEndpoint");
  //    assertThat(config.prometheusExporterHost).isEqualTo("myPrometheusHost");
  //    assertThat(config.prometheusExporterPort).isEqualTo(234);
  //    assertThat(config.username).isEqualTo("myUsername");
  //    assertThat(config.password).isEqualTo("myPassword");
  //    assertThat(config.remoteProfile).isEqualTo("myRemoteProfile");
  //    assertThat(config.realm).isEqualTo("myRealm");
  //  }
  //
  //  @Test
  //  void propertiesFile() {
  //    Properties props = new Properties();
  //    JmxMetrics.loadPropertiesFromPath(
  //        props, ClassLoader.getSystemClassLoader().getResource("all.properties").getPath());
  //    JmxConfig config = new JmxConfig(props);
  //
  //
  // assertThat(config.serviceUrl).isEqualTo("service:jmx:rmi:///jndi/rmi://myhost:12345/jmxrmi");
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
  //    // These properties are set from the config file loading into JmxConfig
  //    assertThat(System.getProperty("javax.net.ssl.keyStore")).isEqualTo("/my/key/store");
  //    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("abc123");
  //    assertThat(System.getProperty("javax.net.ssl.keyStoreType")).isEqualTo("JKS");
  //    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo("/my/trust/store");
  //    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo("def456");
  //    assertThat(System.getProperty("javax.net.ssl.trustStoreType")).isEqualTo("JKS");
  //  }
  //
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
  //  @SetSystemProperty(key = "otel.jmx.interval.milliseconds", value = "abc")
  //  void invalidInterval() {
  //    assertThatThrownBy(JmxConfig::new)
  //        .isInstanceOf(ConfigurationException.class)
  //        .hasMessage("Failed to parse otel.jmx.interval.milliseconds");
  //  }
  //
  //  @Test
  //  @SetSystemProperty(key = "otel.exporter.prometheus.port", value = "abc")
  //  void invalidPrometheusPort() {
  //    assertThatThrownBy(JmxConfig::new)
  //        .isInstanceOf(ConfigurationException.class)
  //        .hasMessage("Failed to parse otel.exporter.prometheus.port");
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
  //  @SetSystemProperty(key = "otel.jmx.service.url", value = "requiredValue")
  //  @SetSystemProperty(key = "otel.jmx.target.system", value = "jvm,unavailableTargetSystem")
  //  void invalidTargetSystem() {
  //    JmxConfig config = new JmxConfig();
  //
  //    assertThatThrownBy(config::validate)
  //        .isInstanceOf(ConfigurationException.class)
  //        .hasMessage(
  //            "[jvm, unavailabletargetsystem] must specify targets from [activemq, cassandra,
  // hbase, hadoop, jetty, jvm, "
  //                + "kafka, kafka-consumer, kafka-producer, solr, tomcat, wildfly]");
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
