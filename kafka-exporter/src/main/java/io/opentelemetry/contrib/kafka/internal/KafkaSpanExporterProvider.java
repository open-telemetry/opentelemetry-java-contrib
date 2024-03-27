/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.internal;

import io.opentelemetry.contrib.kafka.KafkaSpanExporter;
import io.opentelemetry.contrib.kafka.KafkaSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

// import java.time.Duration;

/**
 * {@link SpanExporter} SPI implementation for {@link KafkaSpanExporter}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class KafkaSpanExporterProvider implements ConfigurableSpanExporterProvider {
  @Override
  public SpanExporter createExporter(ConfigProperties config) {
    KafkaSpanExporterBuilder kafkaSpanExporterBuilder = KafkaSpanExporter.newBuilder();
    // String protocolVersion = config.getString("exporter.kafka.spans.protocolversion", "2.0.0");
    // String brokers = config.getString("exporter.kafka.spans.brokers", "localhost:9092");
    // String topic = config.getString("exporter.kafka.spans.topic", "otlp_spans");
    // long timeout = config.getLong("exporter.kafka.spans.timeout", 1000L);
    // kafkaSpanExporterBuilder.setProtocolVersion(protocolVersion);
    // kafkaSpanExporterBuilder.setBrokers(brokers);
    // kafkaSpanExporterBuilder.setTopic(topic);
    // kafkaSpanExporterBuilder.setTimeout(Duration.ofMillis(timeout));
    return kafkaSpanExporterBuilder.build();
  }

  @Override
  public String getName() {
    return "kafka";
  }
}
