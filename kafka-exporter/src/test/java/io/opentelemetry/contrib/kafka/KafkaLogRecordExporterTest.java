/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
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
class KafkaLogRecordExporterTest {

  private static final Resource RESOURCE =
      Resource.create(Attributes.builder().put("key", "value").build());

  private static final LogRecordData LOG1 =
      TestLogRecordData.builder()
          .setResource(RESOURCE)
          .setInstrumentationScopeInfo(
              InstrumentationScopeInfo.builder("instrumentation")
                  .setVersion("1")
                  .setAttributes(Attributes.builder().put("key", "value").build())
                  .build())
          .setBody("body1")
          .setSeverity(Severity.INFO)
          .setSeverityText("INFO")
          .setTimestamp(100L, TimeUnit.NANOSECONDS)
          .setObservedTimestamp(200L, TimeUnit.NANOSECONDS)
          .setAttributes(Attributes.of(stringKey("animal"), "cat", longKey("lives"), 9L))
          .setSpanContext(
              SpanContext.create(
                  "12345678876543211234567887654322",
                  "8765432112345876",
                  TraceFlags.getDefault(),
                  TraceState.getDefault()))
          .build();

  private static final LogRecordData LOG2 =
      TestLogRecordData.builder()
          .setResource(RESOURCE)
          .setInstrumentationScopeInfo(
              InstrumentationScopeInfo.builder("instrumentation2").setVersion("2").build())
          .setBody("body2")
          .setSeverity(Severity.INFO)
          .setSeverityText("INFO")
          .setTimestamp(100L, TimeUnit.NANOSECONDS)
          .setObservedTimestamp(200L, TimeUnit.NANOSECONDS)
          .setAttributes(Attributes.of(booleanKey("important"), true))
          .setSpanContext(
              SpanContext.create(
                  "12345678876543211234567887654322",
                  "8765432112345875",
                  TraceFlags.getDefault(),
                  TraceState.getDefault()))
          .build();

  private static final DockerImageName KAFKA_TEST_IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

  private KafkaContainer kafka;

  private KafkaConsumer<String, Bytes> consumer;

  private LogRecordExporter exporter;

  @BeforeAll
  void initialize() throws Exception {
    kafka = new KafkaContainer(KAFKA_TEST_IMAGE);
    kafka.start();
    String bootstrapServers = kafka.getBootstrapServers();
    KafkaLogRecordExporterBuilder kafkaLogRecordExporterBuilder = KafkaLogRecordExporter.builder();
    kafkaLogRecordExporterBuilder.setProtocolVersion("2.0.0");
    kafkaLogRecordExporterBuilder.setBrokers(bootstrapServers);
    kafkaLogRecordExporterBuilder.setTopic("otlp_logs");
    kafkaLogRecordExporterBuilder.setTimeout(Duration.ofSeconds(1L));
    exporter = kafkaLogRecordExporterBuilder.build();

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

      List<NewTopic> topics = Collections.singletonList(new NewTopic("otlp_logs", 1, (short) 1));
      adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

      consumer.subscribe(Collections.singletonList("otlp_logs"));
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
  void export() throws Exception {
    CompletableResultCode actual = exporter.export(Arrays.asList(LOG1, LOG2));

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

          LogsRequestMarshaler logsRequestMarshaler =
              LogsRequestMarshaler.create(Arrays.asList(LOG1, LOG2));
          byte[] byteArray = new byte[logsRequestMarshaler.getBinarySerializedSize()];
          ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
          OutputStream outputStream = new ByteBufferOutputStream(byteBuffer);
          logsRequestMarshaler.writeBinaryTo(outputStream);
          Bytes expected = new Bytes(byteArray);

          Assertions.assertThat(records)
              .hasSize(1)
              .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
              .containsExactly(Assertions.tuple("otlp_logs", null, expected));

          return true;
        });
  }
}
