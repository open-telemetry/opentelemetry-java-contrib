/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

class JmxScraperConfigFactoryTest {
  private static Properties validProperties;

  @BeforeAll
  static void setUp() {
    validProperties = new Properties();
    validProperties.setProperty(
        JmxScraperConfigFactory.SERVICE_URL,
        "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    validProperties.setProperty(JmxScraperConfigFactory.CUSTOM_JMX_SCRAPING_CONFIG, "");
    validProperties.setProperty(JmxScraperConfigFactory.TARGET_SYSTEM, "tomcat, activemq");
    validProperties.setProperty(JmxScraperConfigFactory.METRICS_EXPORTER_TYPE, "otel");
    validProperties.setProperty(JmxScraperConfigFactory.INTERVAL_MILLISECONDS, "1410");
    validProperties.setProperty(JmxScraperConfigFactory.REGISTRY_SSL, "true");
    validProperties.setProperty(JmxScraperConfigFactory.OTLP_ENDPOINT, "http://localhost:4317");
    validProperties.setProperty(JmxScraperConfigFactory.JMX_USERNAME, "some-user");
    validProperties.setProperty(JmxScraperConfigFactory.JMX_PASSWORD, "some-password");
    validProperties.setProperty(JmxScraperConfigFactory.JMX_REMOTE_PROFILE, "some-profile");
    validProperties.setProperty(JmxScraperConfigFactory.JMX_REALM, "some-realm");
  }

  @Test
  void shouldCreateMinimalValidConfiguration() throws ConfigurationException {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = new Properties();
    properties.setProperty(
        JmxScraperConfigFactory.SERVICE_URL,
        "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    properties.setProperty(JmxScraperConfigFactory.CUSTOM_JMX_SCRAPING_CONFIG, "/file.properties");

    // When
    JmxScraperConfig config = configFactory.createConfig(properties);

    // Then
    assertThat(config.serviceUrl).isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    assertThat(config.customJmxScrapingConfigPath).isEqualTo("/file.properties");
    assertThat(config.targetSystems).isEmpty();
    assertThat(config.intervalMilliseconds).isEqualTo(10000);
    assertThat(config.metricsExporterType).isEqualTo("logging");
    assertThat(config.otlpExporterEndpoint).isNull();
    assertThat(config.username).isNull();
    assertThat(config.password).isNull();
    assertThat(config.remoteProfile).isNull();
    assertThat(config.realm).isNull();
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
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    // Properties to be propagated to system, properties
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty("javax.net.ssl.keyStore", "/my/key/store");
    properties.setProperty("javax.net.ssl.keyStorePassword", "abc123");
    properties.setProperty("javax.net.ssl.keyStoreType", "JKS");
    properties.setProperty("javax.net.ssl.trustStore", "/my/trust/store");
    properties.setProperty("javax.net.ssl.trustStorePassword", "def456");
    properties.setProperty("javax.net.ssl.trustStoreType", "JKS");

    // When
    JmxScraperConfig config = configFactory.createConfig(properties);

    // Then
    assertThat(config.serviceUrl).isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    assertThat(config.customJmxScrapingConfigPath).isEqualTo("");
    assertThat(config.targetSystems).containsOnly("tomcat", "activemq");
    assertThat(config.intervalMilliseconds).isEqualTo(1410);
    assertThat(config.metricsExporterType).isEqualTo("otel");
    assertThat(config.otlpExporterEndpoint).isEqualTo("http://localhost:4317");
    assertThat(config.username).isEqualTo("some-user");
    assertThat(config.password).isEqualTo("some-password");
    assertThat(config.remoteProfile).isEqualTo("some-profile");
    assertThat(config.realm).isEqualTo("some-realm");
    assertThat(config.registrySsl).isTrue();

    // These properties are set from the config file loading into JmxConfig
    assertThat(System.getProperty("javax.net.ssl.keyStore")).isEqualTo("/my/key/store");
    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("abc123");
    assertThat(System.getProperty("javax.net.ssl.keyStoreType")).isEqualTo("JKS");
    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo("/my/trust/store");
    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo("def456");
    assertThat(System.getProperty("javax.net.ssl.trustStoreType")).isEqualTo("JKS");
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.service.url", value = "originalServiceUrl")
  @SetSystemProperty(key = "javax.net.ssl.keyStorePassword", value = "originalPassword")
  void shouldRetainPredefinedSystemProperties() throws ConfigurationException {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    // Properties to be propagated to system, properties
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty("javax.net.ssl.keyStorePassword", "abc123");

    // When
    configFactory.createConfig(properties);

    // Then
    assertThat(System.getProperty("otel.jmx.service.url")).isEqualTo("originalServiceUrl");
    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("originalPassword");
  }

  @Test
  void shouldFailValidation_missingServiceUrl() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfigFactory.SERVICE_URL);

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.service.url must be specified.");
  }

  @Test
  void shouldFailValidation_missingConfigPathAndTargetSystem() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfigFactory.CUSTOM_JMX_SCRAPING_CONFIG);
    properties.remove(JmxScraperConfigFactory.TARGET_SYSTEM);

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "otel.jmx.custom.jmx.scraping.config or otel.jmx.target.system must be specified.");
  }

  @Test
  void shouldFailValidation_invalidTargetSystem() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfigFactory.TARGET_SYSTEM, "hal9000");

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "[hal9000] must specify targets from "
                + JmxScraperConfigFactory.AVAILABLE_TARGET_SYSTEMS);
  }

  @Test
  void shouldFailValidation_missingOtlpEndpoint() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JmxScraperConfigFactory.OTLP_ENDPOINT);
    properties.setProperty(JmxScraperConfigFactory.METRICS_EXPORTER_TYPE, "otlp");

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.exporter.otlp.endpoint must be specified for otlp format.");
  }

  @Test
  void shouldFailValidation_negativeInterval() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfigFactory.INTERVAL_MILLISECONDS, "-1");

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("otel.jmx.interval.milliseconds must be positive.");
  }

  @Test
  void shouldFailConfigCreation_invalidInterval() {
    // Given
    JmxScraperConfigFactory configFactory = new JmxScraperConfigFactory();
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JmxScraperConfigFactory.INTERVAL_MILLISECONDS, "abc");

    // When and Then
    assertThatThrownBy(() -> configFactory.createConfig(properties))
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
