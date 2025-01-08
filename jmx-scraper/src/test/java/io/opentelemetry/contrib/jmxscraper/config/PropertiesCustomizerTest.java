/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesCustomizerTest {

  @Test
  void tryGetConfigBeforeApply() {
    assertThatThrownBy(()-> new PropertiesCustomizer().getScraperConfig())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void defaultLoggingExporter() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", "dummy-url");
    map.put("otel.jmx.target.system", "jvm");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config)).containsEntry("otel.metrics.exporter", "logging");
  }

  @Test
  void explicitExporterSet() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", "dummy-url");
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp,logging");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config)).isEmpty();
  }

  @Test
  void getDefaultConfig() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", "dummy-url");
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config)).isEmpty();

    assertThat(customizer.getScraperConfig()).isNotNull();
  }
}
