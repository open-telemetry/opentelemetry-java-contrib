/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.internal;

import io.opentelemetry.contrib.kafka.KafkaMetricExporter;
import io.opentelemetry.contrib.kafka.KafkaMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.time.Duration;

/**
 * {@link MetricExporter} SPI implementation for {@link KafkaMetricExporter}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class KafkaMetricExporterProvider implements ConfigurableMetricExporterProvider {
  @Override
  public MetricExporter createExporter(ConfigProperties config) {
    KafkaMetricExporterBuilder kafkaMetricExporterBuilder = KafkaMetricExporter.builder();
    String protocolVersion = config.getString("exporter.kafka.metrics.protocolversion", "2.0.0");
    String brokers = config.getString("exporter.kafka.metrics.brokers", "localhost:9092");
    String topic = config.getString("exporter.kafka.metrics.topic", "otlp_metrics");
    long timeout = config.getLong("exporter.kafka.metrics.timeout", 1000L);
    kafkaMetricExporterBuilder.setProtocolVersion(protocolVersion);
    kafkaMetricExporterBuilder.setBrokers(brokers);
    kafkaMetricExporterBuilder.setTopic(topic);
    kafkaMetricExporterBuilder.setTimeout(Duration.ofMillis(timeout));
    return kafkaMetricExporterBuilder.build();
  }

  @Override
  public String getName() {
    return "kafka";
  }
}
