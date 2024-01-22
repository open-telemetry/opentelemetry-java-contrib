/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.internal;

import io.opentelemetry.contrib.kafka.KafkaLogRecordExporter;
import io.opentelemetry.contrib.kafka.KafkaLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.time.Duration;

/**
 * {@link LogRecordExporter} SPI implementation for {@link KafkaLogRecordExporter}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class KafkaLogRecordExporterProvider implements ConfigurableLogRecordExporterProvider {
  @Override
  public LogRecordExporter createExporter(ConfigProperties config) {
    KafkaLogRecordExporterBuilder kafkaLogRecordExporterBuilder = KafkaLogRecordExporter.builder();
    String protocolVersion = config.getString("exporter.kafka.logs.protocolversion", "2.0.0");
    String brokers = config.getString("exporter.kafka.logs.brokers", "localhost:9092");
    String topic = config.getString("exporter.kafka.logs.topic", "otlp_logs");
    long timeout = config.getLong("exporter.kafka.logs.timeout", 1000L);
    kafkaLogRecordExporterBuilder.setProtocolVersion(protocolVersion);
    kafkaLogRecordExporterBuilder.setBrokers(brokers);
    kafkaLogRecordExporterBuilder.setTopic(topic);
    kafkaLogRecordExporterBuilder.setTimeout(Duration.ofMillis(timeout));
    return kafkaLogRecordExporterBuilder.build();
  }

  @Override
  public String getName() {
    return "kafka";
  }
}
