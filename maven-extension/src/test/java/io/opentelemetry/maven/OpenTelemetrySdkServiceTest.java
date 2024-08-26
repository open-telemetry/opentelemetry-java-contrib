/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

public class OpenTelemetrySdkServiceTest {

  /** Verify default config */
  @Test
  public void testDefaultConfiguration() {
    System.clearProperty("otel.exporter.otlp.endpoint");
    System.clearProperty("otel.service.name");
    System.clearProperty("otel.resource.attributes");
    try (OpenTelemetrySdkService openTelemetrySdkService = new OpenTelemetrySdkService()) {

      Resource resource = openTelemetrySdkService.getResource();
      assertThat(resource.getAttribute(stringKey("service.name"))).isEqualTo("maven");

      ConfigProperties configProperties = openTelemetrySdkService.getConfigProperties();
      assertThat(configProperties.getString("otel.exporter.otlp.endpoint")).isNull();
      assertThat(configProperties.getString("otel.traces.exporter")).isEqualTo("none");
      assertThat(configProperties.getString("otel.metrics.exporter")).isEqualTo("none");
      assertThat(configProperties.getString("otel.logs.exporter")).isEqualTo("none");
    }
  }

  /** Verify overwritten `service.name`,`key1` and `key2` */
  @Test
  public void testOverwrittenResourceAttributes() {
    System.setProperty("otel.service.name", "my-maven");
    System.setProperty("otel.resource.attributes", "key1=val1,key2=val2");

      try (OpenTelemetrySdkService openTelemetrySdkService = new OpenTelemetrySdkService()) {

        Resource resource = openTelemetrySdkService.getResource();
        assertThat(resource.getAttribute(stringKey("service.name"))).isEqualTo("my-maven");
        assertThat(resource.getAttribute(stringKey("key1"))).isEqualTo("val1");
        assertThat(resource.getAttribute(stringKey("key2"))).isEqualTo("val2");

    } finally {
        System.clearProperty("otel.service.name");
        System.clearProperty("otel.resource.attributes");
    }
  }

  /** Verify overwritten `"otel.exporter.otlp.endpoint" */
  @Test
  public void testOverwrittenExporterConfiguration_1() {
    System.setProperty("otel.exporter.otlp.endpoint", "http://example.com:4318");

    try (OpenTelemetrySdkService openTelemetrySdkService = new OpenTelemetrySdkService()) {


      ConfigProperties configProperties = openTelemetrySdkService.getConfigProperties();
      assertThat(configProperties.getString("otel.exporter.otlp.endpoint")).isEqualTo("http://example.com:4318");
      assertThat(configProperties.getString("otel.traces.exporter")).isNull();
      assertThat(configProperties.getString("otel.metrics.exporter")).isNull();
      assertThat(configProperties.getString("otel.logs.exporter")).isNull();

    } finally {
      System.clearProperty("otel.exporter.otlp.endpoint");
    }
  }

  /** Verify overwritten `"otel.exporter.otlp.traces.endpoint" */
  @Test
  public void testOverwrittenExporterConfiguration_2() {
    System.clearProperty("otel.exporter.otlp.endpoint");
    System.setProperty("otel.exporter.otlp.traces.endpoint", "http://example.com:4318/v1/traces");
    System.setProperty("otel.exporter.otlp.traces.protocol", "http/protobuf");

    try (OpenTelemetrySdkService openTelemetrySdkService = new OpenTelemetrySdkService()) {

      ConfigProperties configProperties = openTelemetrySdkService.getConfigProperties();
      assertThat(configProperties.getString("otel.exporter.otlp.endpoint")).isNull();
      assertThat(configProperties.getString("otel.exporter.otlp.traces.endpoint")).isEqualTo("http://example.com:4318/v1/traces");
      assertThat(configProperties.getString("otel.traces.exporter")).isNull();
      assertThat(configProperties.getString("otel.metrics.exporter")).isEqualTo("none");
      assertThat(configProperties.getString("otel.logs.exporter")).isEqualTo("none");

    } finally {
      System.clearProperty("otel.exporter.otlp.endpoint");
      System.clearProperty("otel.exporter.otlp.traces.endpoint");
      System.clearProperty("otel.exporter.otlp.traces.protocol");
    }
  }
}
