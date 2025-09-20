/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesCustomizerTest {

  private static final String DUMMY_URL = "service:jmx:rmi:///jndi/rmi://host:999/jmxrmi";

  @Test
  void tryGetBeforeApply() {
    assertThatThrownBy(() -> new PropertiesCustomizer().getScraperConfig())
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new PropertiesCustomizer().getConnectorBuilder())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void defaultOtlpExporter() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", DUMMY_URL);
    map.put("otel.jmx.target.system", "jvm");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config)).containsEntry("otel.metrics.exporter", "otlp");
  }

  @Test
  void explicitExporterSet() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", DUMMY_URL);
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp,logging");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config)).isEmpty();
  }

  @Test
  void getSomeConfiguration() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", DUMMY_URL);
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config))
        .describedAs("sdk configuration should not be overridden")
        .isEmpty();

    JmxScraperConfig scraperConfig = customizer.getScraperConfig();
    assertThat(scraperConfig).isNotNull();
    assertThat(scraperConfig.getTargetSystems()).containsOnly("jvm");
  }

  @Test
  void setSdkMetricExportFromJmxInterval() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", DUMMY_URL);
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp");
    map.put("otel.jmx.interval.milliseconds", "10000");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config))
        .describedAs("sdk export interval must be set")
        .hasSize(1)
        .containsEntry("otel.metric.export.interval", "10000ms");
  }

  @Test
  void sdkMetricExportIntervalPriority() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.jmx.service.url", DUMMY_URL);
    map.put("otel.jmx.target.system", "jvm");
    map.put("otel.metrics.exporter", "otlp");
    map.put("otel.jmx.interval.milliseconds", "10000");
    map.put("otel.metric.export.interval", "15s");
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);

    PropertiesCustomizer customizer = new PropertiesCustomizer();
    assertThat(customizer.apply(config))
        .describedAs("provided sdk export interval must keep provided value")
        .isEmpty();

    assertThat(customizer.getScraperConfig().getSamplingInterval())
        .describedAs("jmx export interval must be ignored")
        .isEqualTo(Duration.ofSeconds(15));
  }
}
