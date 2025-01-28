/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_CONFIG;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_CONFIG_LEGACY;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_PASSWORD;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_REALM;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_REGISTRY_SSL;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_REMOTE_PROFILE;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_SERVICE_URL;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_TARGET_SYSTEM;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_USERNAME;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.fromConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JmxScraperConfigTest {
  private static Properties validProperties;

  @BeforeAll
  static void setUp() {
    validProperties = new Properties();
    validProperties.setProperty(
        JMX_SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    validProperties.setProperty(JMX_CONFIG, "/path/to/config.yaml");
    validProperties.setProperty(JMX_TARGET_SYSTEM, "tomcat, activemq");
    validProperties.setProperty(JMX_REGISTRY_SSL, "true");
    validProperties.setProperty(JMX_USERNAME, "some-user");
    validProperties.setProperty(JMX_PASSWORD, "some-password");
    validProperties.setProperty(JMX_REMOTE_PROFILE, "some-profile");
    validProperties.setProperty(JMX_REALM, "some-realm");
    // otel sdk metric export interval
    validProperties.setProperty("otel.metric.export.interval", "10s");
  }

  @Test
  void shouldPassValidation() {
    // When
    JmxScraperConfig config = fromConfig(TestUtil.configProperties(validProperties));

    // Then
    assertThat(config.getServiceUrl())
        .isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    assertThat(config.getJmxConfig()).containsExactly("/path/to/config.yaml");
    assertThat(config.getTargetSystems()).containsExactlyInAnyOrder("tomcat", "activemq");
    assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getUsername()).isEqualTo("some-user");
    assertThat(config.getPassword()).isEqualTo("some-password");
    assertThat(config.getRemoteProfile()).isEqualTo("some-profile");
    assertThat(config.getRealm()).isEqualTo("some-realm");
  }

  @ParameterizedTest(name = "custom yaml = {arguments}")
  @ValueSource(booleans = {true, false})
  public void shouldCreateMinimalValidConfiguration(boolean customYaml){
    // Given
    Properties properties = new Properties();
    properties.setProperty(JMX_SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    if(customYaml){
      properties.setProperty(JMX_CONFIG, "/file.yaml");
    } else {
      properties.setProperty(JMX_TARGET_SYSTEM, "tomcat");
    }

    // When
    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    // Then
    assertThat(config.getServiceUrl())
        .isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");

    if(customYaml){
      assertThat(config.getJmxConfig()).containsExactly("/file.yaml");
      assertThat(config.getTargetSystems()).isEmpty();
    } else {
      assertThat(config.getJmxConfig()).isEmpty();
      assertThat(config.getTargetSystems()).containsExactly("tomcat");
    }


    assertThat(config.getSamplingInterval())
        .describedAs("default sampling interval must align to default metric export interval")
        .isEqualTo(Duration.ofMinutes(1));
    assertThat(config.getUsername()).isNull();
    assertThat(config.getPassword()).isNull();
    assertThat(config.getRemoteProfile()).isNull();
    assertThat(config.getRealm()).isNull();
  }

  @Test
  void legacyCustomScrapingConfig() {
    // Given
    Properties properties = new Properties();
    properties.setProperty(JMX_SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    properties.setProperty(JMX_CONFIG_LEGACY, "/file.yaml");
    properties.setProperty(JMX_CONFIG, "/another.yaml");

    // When
    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    // Then
    assertThat(config.getJmxConfig()).containsOnly("/file.yaml", "/another.yaml");
  }

  @Test
  void shouldFailValidation_missingServiceUrl() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JMX_SERVICE_URL);

    // When and Then
    assertThatThrownBy(() -> fromConfig(TestUtil.configProperties(properties)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("missing mandatory otel.jmx.service.url");
  }

  @Test
  void shouldFailValidation_missingConfigPathAndTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.remove(JMX_CONFIG);
    properties.remove(JMX_TARGET_SYSTEM);

    // When and Then
    assertThatThrownBy(() -> fromConfig(TestUtil.configProperties(properties)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "at least one of 'otel.jmx.target.system' or 'otel.jmx.config' must be set");
  }

  @Test
  void shouldFailValidation_invalidTargetSystem() {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JMX_TARGET_SYSTEM, "hal9000");

    // When and Then
    assertThatThrownBy(() -> fromConfig(TestUtil.configProperties(properties)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageStartingWith("unsupported target system");
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
