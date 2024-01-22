/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.resources.Resource;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.ByteBufferOutputStream;
import org.apache.kafka.common.utils.Bytes;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaMetricExporterTest {

  private static final Resource RESOURCE =
      Resource.create(Attributes.builder().put("key", "value").build());

  private static final MetricData METRIC1 =
      ImmutableMetricData.createDoubleSum(
          RESOURCE,
          InstrumentationScopeInfo.builder("instrumentation")
              .setVersion("1")
              .setAttributes(Attributes.builder().put("key", "value").build())
              .build(),
          "metric1",
          "metric1 description",
          "m",
          ImmutableSumData.create(
              true,
              AggregationTemporality.CUMULATIVE,
              Arrays.asList(
                  ImmutableDoublePointData.create(
                      1, 2, Attributes.of(stringKey("cat"), "meow"), 4))));

  private static final MetricData METRIC2 =
      ImmutableMetricData.createDoubleSum(
          RESOURCE,
          InstrumentationScopeInfo.builder("instrumentation2").setVersion("2").build(),
          "metric2",
          "metric2 description",
          "s",
          ImmutableSumData.create(
              true,
              AggregationTemporality.CUMULATIVE,
              Arrays.asList(
                  ImmutableDoublePointData.create(
                      1, 2, Attributes.of(stringKey("cat"), "meow"), 4))));

  private static final DockerImageName KAFKA_TEST_IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

  private KafkaContainer kafka;

  private KafkaConsumer<String, Bytes> consumer;

  private MetricExporter exporter;

  @BeforeAll
  void initialize() throws Exception {
    kafka = new KafkaContainer(KAFKA_TEST_IMAGE);
    kafka.start();
    String bootstrapServers = kafka.getBootstrapServers();
    KafkaMetricExporterBuilder kafkaMetricExporterBuilder = KafkaMetricExporter.builder();
    kafkaMetricExporterBuilder.setProtocolVersion("2.0.0");
    kafkaMetricExporterBuilder.setBrokers(bootstrapServers);
    kafkaMetricExporterBuilder.setTopic("otlp_metrics");
    kafkaMetricExporterBuilder.setTimeout(Duration.ofSeconds(1L));
    exporter = kafkaMetricExporterBuilder.build();

    try (AdminClient adminClient =
        AdminClient.create(
            ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
      consumer =
          new KafkaConsumer<String, Bytes>(
              ImmutableMap.of(
                  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  bootstrapServers,
                  ConsumerConfig.GROUP_ID_CONFIG,
                  "tc-" + UUID.randomUUID(),
                  ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                  "earliest"),
              new StringDeserializer(),
              new BytesDeserializer());

      List<NewTopic> topics = Collections.singletonList(new NewTopic("otlp_metrics", 1, (short) 1));
      adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

      consumer.subscribe(Collections.singletonList("otlp_metrics"));
    }
  }

  @AfterAll
  void cleanup() {
    Awaitility.await()
        .untilAsserted(() -> Assertions.assertThat(exporter.shutdown().isSuccess()).isTrue());
    Awaitility.await()
        .untilAsserted(
            () ->
                Assertions.assertThat(exporter.export(Collections.emptyList()).isSuccess())
                    .isFalse());
    consumer.unsubscribe();
    kafka.close();
  }

  @Test
  void getAggregationTemporality() {
    Assertions.assertThat(exporter.getAggregationTemporality(InstrumentType.COUNTER))
        .isEqualTo(AggregationTemporality.CUMULATIVE);
  }

  @Test
  void export() throws Exception {
    CompletableResultCode actual = exporter.export(Arrays.asList(METRIC1, METRIC2));

    Awaitility.await()
        .untilAsserted(
            () -> {
              Assertions.assertThat(actual.isSuccess()).isTrue();
              Assertions.assertThat(actual.isDone()).isTrue();
            });
    Unreliables.retryUntilTrue(
        10,
        TimeUnit.SECONDS,
        () -> {
          ConsumerRecords<String, Bytes> records = consumer.poll(Duration.ofMillis(100));

          if (records.isEmpty()) {
            return false;
          }

          MetricsRequestMarshaler metricsRequestMarshaler =
              MetricsRequestMarshaler.create(Arrays.asList(METRIC1, METRIC2));
          byte[] byteArray = new byte[metricsRequestMarshaler.getBinarySerializedSize()];
          ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
          OutputStream outputStream = new ByteBufferOutputStream(byteBuffer);
          metricsRequestMarshaler.writeBinaryTo(outputStream);
          Bytes expected = new Bytes(byteArray);

          Assertions.assertThat(records)
              .hasSize(1)
              .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
              .containsExactly(Assertions.tuple("otlp_metrics", null, expected));

          return true;
        });
  }

  @Test
  void flush() {
    Assertions.assertThat(exporter.flush().isSuccess()).isTrue();
  }
}
