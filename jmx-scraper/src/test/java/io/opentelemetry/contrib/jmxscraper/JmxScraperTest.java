/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.jmxscraper.config.JmxScraperConfigFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class JmxScraperTest {
  @Test
  void shouldThrowExceptionWhenInvalidCommandLineArgsProvided() {
    // Given
    List<String> emptyArgs = Collections.singletonList("-inexistingOption");
    JmxScraperConfigFactory configFactoryMock = mock(JmxScraperConfigFactory.class);

    // When and Then
    assertThatThrownBy(() -> JmxScraper.createConfigFromArgs(emptyArgs, configFactoryMock))
        .isInstanceOf(ArgumentsParsingException.class);
  }

  @Test
  void shouldThrowExceptionWhenTooManyCommandLineArgsProvided() {
    // Given
    List<String> emptyArgs = Arrays.asList("-config", "path", "-inexistingOption");
    JmxScraperConfigFactory configFactoryMock = mock(JmxScraperConfigFactory.class);

    // When and Then
    assertThatThrownBy(() -> JmxScraper.createConfigFromArgs(emptyArgs, configFactoryMock))
        .isInstanceOf(ArgumentsParsingException.class);
  }
}
