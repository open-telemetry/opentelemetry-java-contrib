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
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_TARGET_SOURCE;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_TARGET_SYSTEM;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.JMX_USERNAME;
import static io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig.fromConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
  public void shouldCreateMinimalValidConfiguration(boolean customYaml) {
    // Given
    Properties properties = new Properties();
    properties.setProperty(JMX_SERVICE_URL, "jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");
    if (customYaml) {
      properties.setProperty(JMX_CONFIG, "/file.yaml");
    } else {
      properties.setProperty(JMX_TARGET_SYSTEM, "tomcat");
    }

    // When
    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    // Then
    assertThat(config.getServiceUrl())
        .isEqualTo("jservice:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi");

    if (customYaml) {
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
        .hasMessage("at least one of 'otel.jmx.target.system' or 'otel.jmx.config' must be set");
  }

  @ParameterizedTest
  @EnumSource(JmxScraperConfig.TargetSystemSource.class)
  void shouldFailValidation_invalidTargetSystem(JmxScraperConfig.TargetSystemSource source) {
    // Given
    Properties properties = (Properties) validProperties.clone();
    properties.setProperty(JMX_TARGET_SYSTEM, "hal9000");
    properties.setProperty(JMX_TARGET_SOURCE, source.name().toLowerCase(Locale.ROOT));

    // When and Then
    assertThatThrownBy(() -> fromConfig(TestUtil.configProperties(properties)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageStartingWith("unsupported target system");
  }

  @ParameterizedTest
  @ValueSource(strings = {"auto", ""})
  void targetSystemSource_auto(String source) {
    Properties properties = (Properties) validProperties.clone();
    // we just need to provide a valid value for parsing
    properties.setProperty(JMX_TARGET_SYSTEM, "fake-test-system-both");
    if (!source.isEmpty()) {
      properties.setProperty(JMX_TARGET_SOURCE, source);
    }

    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    // should resolve to instrumentation when available in both
    shouldResolveToInstrumentationYaml(config, "fake-test-system-both");

    // should resolve to legacy yaml when not available in instrumentation
    shouldResolveToLegacyYaml(config, "fake-test-system-legacy-only");

    // should resolve to instrumentation when only defined there
    shouldResolveToInstrumentationYaml(config, "fake-test-system-instrumentation-only");
  }

  @Test
  void targetSystemSource_legacy() {
    Properties properties = (Properties) validProperties.clone();
    // we just need to provide a valid value for parsing
    properties.setProperty(JMX_TARGET_SYSTEM, "fake-test-system-both");
    properties.setProperty(JMX_TARGET_SOURCE, "legacy");

    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    shouldResolveToLegacyYaml(config, "fake-test-system-both");

    shouldResolveToLegacyYaml(config, "fake-test-system-legacy-only");

    // should not support system only defined in instrumentation
    shouldNotResolveYaml(config, "fake-test-system-instrumentation-only");
  }

  @Test
  void targetSystemSource_instrumentation() {
    Properties properties = (Properties) validProperties.clone();
    // we just need to provide a valid value for parsing
    properties.setProperty(JMX_TARGET_SYSTEM, "fake-test-system-both");
    properties.setProperty(JMX_TARGET_SOURCE, "instrumentation");

    JmxScraperConfig config = fromConfig(TestUtil.configProperties(properties));

    shouldResolveToInstrumentationYaml(config, "fake-test-system-both");

    // should not support system only defined in legacy
    shouldNotResolveYaml(config, "fake-test-system-legacy-only");

    shouldResolveToInstrumentationYaml(config, "fake-test-system-instrumentation-only");
  }

  private static InputStream getYaml(String path) {
    return JmxScraperConfigTest.class.getClassLoader().getResourceAsStream(path);
  }

  private static void shouldResolveToInstrumentationYaml(JmxScraperConfig config, String target) {
    assertThat(config.getTargetSystemYaml(target))
        .describedAs("should resolve to instrumentation yaml")
        .hasSameContentAs(getYaml("jmx/rules/" + target + ".yaml"));
  }

  private static void shouldResolveToLegacyYaml(JmxScraperConfig config, String target) {
    assertThat(config.getTargetSystemYaml(target))
        .describedAs("should resolve to legacy yaml")
        .hasSameContentAs(getYaml(target + ".yaml"));
  }

  private static void shouldNotResolveYaml(JmxScraperConfig config, String target) {
    assertThatThrownBy(() -> config.getTargetSystemYaml(target))
        .describedAs("should not support system")
        .isInstanceOf(ConfigurationException.class)
        .hasMessageStartingWith("unsupported target system");
  }
}
