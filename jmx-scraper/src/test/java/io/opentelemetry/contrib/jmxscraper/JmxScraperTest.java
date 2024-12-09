/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.jmxscraper.config.ConfigurationException;
import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class JmxScraperTest {
  @Test
  void shouldThrowExceptionWhenInvalidCommandLineArgsProvided() {
    // Given
    List<String> emptyArgs = Collections.singletonList("-nonExistentOption");

    // When and Then
    assertThatThrownBy(() -> JmxScraper.parseArgs(emptyArgs))
        .isInstanceOf(ArgumentsParsingException.class);
  }

  @Test
  void shouldThrowExceptionWhenTooManyCommandLineArgsProvided() {
    // Given
    List<String> args = Arrays.asList("-config", "path", "-nonExistentOption");

    // When and Then
    assertThatThrownBy(() -> JmxScraper.parseArgs(args))
        .isInstanceOf(ArgumentsParsingException.class);
  }

  @Test
  void shouldCreateConfig_propertiesLoadedFromFile()
      throws ConfigurationException, ArgumentsParsingException {
    // Given
    String filePath =
        ClassLoader.getSystemClassLoader().getResource("validConfig.properties").getPath();
    List<String> args = Arrays.asList("-config", filePath);

    // When
    JmxScraperConfig config =
        JmxScraperConfig.fromProperties(JmxScraper.parseArgs(args), new Properties());

    // Then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldCreateConfig_propertiesLoadedFromStdIn()
      throws ConfigurationException, ArgumentsParsingException, IOException {
    InputStream originalIn = System.in;
    try (InputStream stream =
        ClassLoader.getSystemClassLoader().getResourceAsStream("validConfig.properties")) {
      // Given
      System.setIn(stream);
      List<String> args = Arrays.asList("-config", "-");

      // When
      JmxScraperConfig config =
          JmxScraperConfig.fromProperties(JmxScraper.parseArgs(args), new Properties());

      // Then
      assertThat(config).isNotNull();
    } finally {
      System.setIn(originalIn);
    }
  }
}
