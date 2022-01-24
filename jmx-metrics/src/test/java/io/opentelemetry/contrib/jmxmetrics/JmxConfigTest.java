/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

class JmxConfigTest {

  @Test
  void staticValues() {
    assertThat(JmxConfig.AVAILABLE_TARGET_SYSTEMS)
        .containsOnly(
            "activemq",
            "cassandra",
            "hadoop",
            "jvm",
            "kafka",
            "kafka-consumer",
            "kafka-producer",
            "solr",
            "tomcat");
  }

  @Test
  @ClearSystemProperty(key = "otel.metric.export.interval")
  void defaultValues() {
    JmxConfig config = new JmxConfig();

    assertThat(config.serviceUrl).isNull();
    assertThat(config.groovyScript).isNull();
    assertThat(config.targetSystem).isEmpty();
    assertThat(config.targetSystems).isEmpty();
    assertThat(config.intervalMilliseconds).isEqualTo(10000);
    assertThat(config.metricsExporterType).isEqualTo("logging");
    assertThat(config.otlpExporterEndpoint).isNull();
    assertThat(config.prometheusExporterHost).isEqualTo("0.0.0.0");
    assertThat(config.prometheusExporterPort).isEqualTo(9464);
    assertThat(config.username).isNull();
    assertThat(config.password).isNull();
    assertThat(config.remoteProfile).isNull();
    assertThat(config.realm).isNull();
    assertThat(config.properties.getProperty("otel.metric.export.interval")).isEqualTo("10000");
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.service.url", value = "myServiceUrl")
  @SetSystemProperty(key = "otel.jmx.groovy.script", value = "myGroovyScript")
  @SetSystemProperty(
      key = "otel.jmx.target.system",
      value = "mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem")
  @SetSystemProperty(key = "otel.jmx.interval.milliseconds", value = "123")
  @SetSystemProperty(key = "otel.metrics.exporter", value = "inmemory")
  @SetSystemProperty(key = "otel.exporter.otlp.endpoint", value = "https://myOtlpEndpoint")
  @SetSystemProperty(key = "otel.exporter.prometheus.host", value = "myPrometheusHost")
  @SetSystemProperty(key = "otel.exporter.prometheus.port", value = "234")
  @SetSystemProperty(key = "otel.jmx.username", value = "myUsername")
  @SetSystemProperty(key = "otel.jmx.password", value = "myPassword")
  @SetSystemProperty(key = "otel.jmx.remote.profile", value = "myRemoteProfile")
  @SetSystemProperty(key = "otel.jmx.realm", value = "myRealm")
  void specifiedValues() {
    JmxConfig config = new JmxConfig();

    assertThat(config.serviceUrl).isEqualTo("myServiceUrl");
    assertThat(config.groovyScript).isEqualTo("myGroovyScript");
    assertThat(config.targetSystem)
        .isEqualTo("mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem");
    assertThat(config.targetSystems)
        .containsOnly("mytargetsystem", "myothertargetsystem", "myadditionaltargetsystem");
    assertThat(config.intervalMilliseconds).isEqualTo(123);
    assertThat(config.metricsExporterType).isEqualTo("inmemory");
    assertThat(config.otlpExporterEndpoint).isEqualTo("https://myOtlpEndpoint");
    assertThat(config.prometheusExporterHost).isEqualTo("myPrometheusHost");
    assertThat(config.prometheusExporterPort).isEqualTo(234);
    assertThat(config.username).isEqualTo("myUsername");
    assertThat(config.password).isEqualTo("myPassword");
    assertThat(config.remoteProfile).isEqualTo("myRemoteProfile");
    assertThat(config.realm).isEqualTo("myRealm");
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.interval.milliseconds", value = "abc")
  void invalidInterval() {
    assertThatThrownBy(JmxConfig::new)
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("Failed to parse otel.jmx.interval.milliseconds");
  }

  @Test
  @SetSystemProperty(key = "otel.exporter.prometheus.port", value = "abc")
  void invalidPrometheusPort() {
    assertThatThrownBy(JmxConfig::new)
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("Failed to parse otel.exporter.prometheus.port");
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.service.url", value = "requiredValue")
  @SetSystemProperty(key = "otel.jmx.groovy.script", value = "myGroovyScript")
  @SetSystemProperty(key = "otel.jmx.target.system", value = "myTargetSystem")
  void conflictingScriptAndTargetSystem() {
    JmxConfig config = new JmxConfig();

    assertThatThrownBy(config::validate)
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "Only one of otel.jmx.groovy.script or otel.jmx.target.system can be specified.");
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.service.url", value = "requiredValue")
  @SetSystemProperty(key = "otel.jmx.target.system", value = "jvm,unavailableTargetSystem")
  void invalidTargetSystem() {
    JmxConfig config = new JmxConfig();

    assertThatThrownBy(config::validate)
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "[jvm, unavailabletargetsystem] must specify targets from [activemq, cassandra, hadoop, jvm, "
                + "kafka, kafka-consumer, kafka-producer, solr, tomcat]");
  }

  @Test
  @SetSystemProperty(key = "otel.metric.export.interval", value = "123")
  void otelMetricExportIntervalRespected() {
    JmxConfig config = new JmxConfig();
    assertThat(config.intervalMilliseconds).isEqualTo(10000);
    assertThat(config.properties.getProperty("otel.metric.export.interval")).isEqualTo("123");
  }
}
