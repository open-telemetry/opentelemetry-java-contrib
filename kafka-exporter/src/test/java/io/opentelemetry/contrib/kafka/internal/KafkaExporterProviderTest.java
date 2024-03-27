/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.internal;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.contrib.kafka.KafkaLogRecordExporter;
import io.opentelemetry.contrib.kafka.KafkaMetricExporter;
// import io.opentelemetry.contrib.kafka.KafkaSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class KafkaExporterProviderTest {

  @Test
  void logRecordExporterProvider() {
    KafkaLogRecordExporterProvider provider = new KafkaLogRecordExporterProvider();
    assertThat(provider.getName()).isEqualTo("kafka");
    assertThat(
            provider.createExporter(DefaultConfigProperties.createFromMap(Collections.emptyMap())))
        .isInstanceOf(KafkaLogRecordExporter.class);
  }

  @Test
  void metricExporterProvider() {
    KafkaMetricExporterProvider provider = new KafkaMetricExporterProvider();
    assertThat(provider.getName()).isEqualTo("kafka");
    assertThat(
            provider.createExporter(DefaultConfigProperties.createFromMap(Collections.emptyMap())))
        .isInstanceOf(KafkaMetricExporter.class);
  }

  // @Test
  // void spanExporterProvider() {
  //   KafkaSpanExporterProvider provider = new KafkaSpanExporterProvider();
  //   assertThat(provider.getName()).isEqualTo("kafka");
  //   assertThat(
  // provider.createExporter(DefaultConfigProperties.createFromMap(Collections.emptyMap())))
  //       .isInstanceOf(KafkaSpanExporter.class);
  // }
}
