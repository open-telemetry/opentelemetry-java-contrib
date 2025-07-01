/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import io.opentelemetry.contrib.jmxscraper.config.TestUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junitpioneer.jupiter.ClearSystemProperty;

class JmxScraperTest {

  @Test
  void shouldThrowExceptionWhenInvalidCommandLineArgsProvided() {
    testInvalidArguments("-nonExistentOption");
    testInvalidArguments("-potato", "-config");
    testInvalidArguments("-config", "path", "-nonExistentOption");
  }

  @Test
  void emptyArgumentsAllowed() throws InvalidArgumentException {
    assertThat(JmxScraper.argsToConfig(Collections.emptyList()))
        .describedAs("empty config allowed to use JVM properties")
        .isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenMissingProperties() {
    testInvalidArguments("-config", "missing.properties");
  }

  private static void testInvalidArguments(String... args) {
    assertThatThrownBy(() -> JmxScraper.argsToConfig(Arrays.asList(args)))
        .isInstanceOf(InvalidArgumentException.class);
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void shouldCreateConfig_propertiesLoadedFromFile() throws InvalidArgumentException {
    // Given
    // Windows returns /C:/path/to/file, which is not a valid path for Path.get() in Java.
    String filePath =
        ClassLoader.getSystemClassLoader().getResource("validConfig.properties").getPath();
    List<String> args = Arrays.asList("-config", filePath);

    // When
    Properties parsedConfig = JmxScraper.argsToConfig(args);
    JmxScraperConfig config = JmxScraperConfig.fromConfig(TestUtil.configProperties(parsedConfig));

    // Then
    assertThat(config).isNotNull();
    assertThat(config.getServiceUrl())
        .isEqualTo("service:jmx:rmi:///jndi/rmi://myhost:12345/jmxrmi");
  }

  @Test
  void shouldCreateConfig_propertiesLoadedFromStdIn() throws InvalidArgumentException, IOException {
    InputStream originalIn = System.in;
    try (InputStream stream =
        ClassLoader.getSystemClassLoader().getResourceAsStream("validConfig.properties")) {
      // Given
      System.setIn(stream);
      List<String> args = Arrays.asList("-config", "-");

      // When
      Properties parsedConfig = JmxScraper.argsToConfig(args);
      JmxScraperConfig config =
          JmxScraperConfig.fromConfig(TestUtil.configProperties(parsedConfig));

      // Then
      assertThat(config).isNotNull();
      assertThat(config.getServiceUrl())
          .isEqualTo("service:jmx:rmi:///jndi/rmi://myhost:12345/jmxrmi");
    } finally {
      System.setIn(originalIn);
    }
  }

  @Test
  @ClearSystemProperty(key = "javax.net.ssl.keyStore")
  @ClearSystemProperty(key = "javax.net.ssl.keyStorePassword")
  @ClearSystemProperty(key = "javax.net.ssl.keyStoreType")
  @ClearSystemProperty(key = "javax.net.ssl.trustStore")
  @ClearSystemProperty(key = "javax.net.ssl.trustStorePassword")
  @ClearSystemProperty(key = "javax.net.ssl.trustStoreType")
  void systemPropertiesPropagation() {

    assertThat(System.getProperty("javax.net.ssl.keyStore"))
        .describedAs("keystore config should not be set")
        .isNull();

    Properties properties = new Properties();
    properties.setProperty("javax.net.ssl.keyStore", "/my/key/store");
    properties.setProperty("javax.net.ssl.keyStorePassword", "abc123");
    properties.setProperty("javax.net.ssl.keyStoreType", "JKS");
    properties.setProperty("javax.net.ssl.trustStore", "/my/trust/store");
    properties.setProperty("javax.net.ssl.trustStorePassword", "def456");
    properties.setProperty("javax.net.ssl.trustStoreType", "JKS");

    properties.setProperty("must.be.ignored", "should not be propagated");

    JmxScraper.propagateToSystemProperties(properties);

    assertThat(System.getProperty("javax.net.ssl.keyStore")).isEqualTo("/my/key/store");
    assertThat(System.getProperty("javax.net.ssl.keyStorePassword")).isEqualTo("abc123");
    assertThat(System.getProperty("javax.net.ssl.keyStoreType")).isEqualTo("JKS");
    assertThat(System.getProperty("javax.net.ssl.trustStore")).isEqualTo("/my/trust/store");
    assertThat(System.getProperty("javax.net.ssl.trustStorePassword")).isEqualTo("def456");
    assertThat(System.getProperty("javax.net.ssl.trustStoreType")).isEqualTo("JKS");

    assertThat(System.getProperty("must.be.ignored")).isNull();

    // when already set, current system properties have priority
    properties.setProperty("javax.net.ssl.keyStore", "/another/key/store");
    JmxScraper.propagateToSystemProperties(properties);
    assertThat(System.getProperty("javax.net.ssl.keyStore"))
        .describedAs("already set system properties must be preserved")
        .isEqualTo("/my/key/store");
  }
}
