/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesCustomizerTest {

  @Test
  void defaultConfig() {
    Map<String, String> result = applyConfig(Collections.emptyMap());
    assertThat(result)
        .describedAs("otel metric export interval must be set")
        .containsEntry(JmxScraperConfig.EXPORTER_INTERVAL, "100000");
  }

  private static Map<String, String> applyConfig(Map<String, String> original) {
    return new PropertiesCustomizer().apply(DefaultConfigProperties.createFromMap(original));
  }
}
