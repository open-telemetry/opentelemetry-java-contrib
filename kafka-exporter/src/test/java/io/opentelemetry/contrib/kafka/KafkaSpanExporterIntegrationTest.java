/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.contrib.kafka.TestUtil.makeBasicSpan;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.Collection;
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
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaSpanExporterIntegrationTest {
  private static final DockerImageName KAFKA_TEST_IMAGE =
      DockerImageName.parse("apache/kafka:3.9.1");
  private static final String TOPIC = "span_topic";
  private KafkaContainer kafka;
  private KafkaConsumer<String, ExportTraceServiceRequest> consumer;
  private SpanDataSerializer spanDataSerializer;
  private ImmutableMap<String, Object> producerConfig;
  private KafkaSpanExporter testSubject;

  @BeforeAll
  void setUp() throws Exception {
    spanDataSerializer = new SpanDataSerializer();
    kafka = new KafkaContainer(KAFKA_TEST_IMAGE);
    kafka.start();
    init(kafka.getBootstrapServers());

    producerConfig =
        ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafka.getBootstrapServers(),
            ProducerConfig.CLIENT_ID_CONFIG,
            UUID.randomUUID().toString());

    testSubject =
        KafkaSpanExporter.newBuilder()
            .setTopicName(TOPIC)
            .setExecutorService(newFixedThreadPool(2))
            .setProducer(
                KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                    .setConfig(producerConfig)
                    .setKeySerializer(new StringSerializer())
                    .setValueSerializer(spanDataSerializer)
                    .build())
            .build();
  }

  @AfterAll
  void tearDown() {
    await().untilAsserted(() -> assertThat(testSubject.shutdown().isSuccess()).isTrue());
    await().untilAsserted(() -> assertThat(testSubject.export(emptyList()).isSuccess()).isFalse());
    consumer.unsubscribe();
    kafka.close();
  }

  @Test
  void export() {
    ImmutableList<SpanData> spans =
        ImmutableList.of(makeBasicSpan("span-1"), makeBasicSpan("span-2"));

    CompletableResultCode actual = testSubject.export(spans);

    await()
        .untilAsserted(
            () -> {
              assertThat(actual.isSuccess()).isTrue();
              assertThat(actual.isDone()).isTrue();
            });
    Unreliables.retryUntilTrue(
        10,
        TimeUnit.SECONDS,
        () -> {
          ConsumerRecords<String, ExportTraceServiceRequest> records =
              consumer.poll(Duration.ofMillis(100));

          if (records.isEmpty()) {
            return false;
          }

          ExportTraceServiceRequest expected = spanDataSerializer.convertSpansToRequest(spans);

          assertThat(records)
              .hasSize(1)
              .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
              .containsExactly(tuple(TOPIC, null, expected));

          return true;
        });
  }

  @Test
  void exportWhenProducerInError() {
    testSubject =
        KafkaSpanExporter.newBuilder()
            .setTopicName(TOPIC)
            .setProducer(
                KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                    .setConfig(producerConfig)
                    .setKeySerializer(new StringSerializer())
                    .setValueSerializer(
                        (topic, data) -> {
                          throw new ApiException("Producer error");
                        })
                    .build())
            .build();

    ImmutableList<SpanData> spans =
        ImmutableList.of(makeBasicSpan("span-1"), makeBasicSpan("span-2"));

    CompletableResultCode actual = testSubject.export(spans);

    await()
        .untilAsserted(
            () -> {
              assertThat(actual.isSuccess()).isFalse();
              assertThat(actual.isDone()).isTrue();
            });

    testSubject.shutdown();
  }

  @Test
  void exportWhenProducerFailsToSend() {
    var mockProducer = new MockProducer<String, Collection<SpanData>>();
    mockProducer.sendException = new KafkaException("Simulated kafka exception");
    var testSubjectWithMockProducer =
        KafkaSpanExporter.newBuilder()
            .setTopicName(TOPIC)
            .setProducer(mockProducer)
            .build();

    ImmutableList<SpanData> spans =
        ImmutableList.of(makeBasicSpan("span-1"), makeBasicSpan("span-2"));

    CompletableResultCode actual = testSubjectWithMockProducer.export(spans);

    await()
        .untilAsserted(
            () -> {
              assertThat(actual.isSuccess()).isFalse();
              assertThat(actual.isDone()).isTrue();
            });

    testSubjectWithMockProducer.shutdown();
  }

  @Test
  void flush() {
    CompletableResultCode actual = testSubject.flush();
    Unreliables.retryUntilTrue(
        20,
        TimeUnit.SECONDS,
        () -> {
          if (!actual.isDone()) {
            return false;
          }

          assertThat(actual.isSuccess()).isTrue();
          return true;
        });
  }

  private void init(String bootstrapServers) throws Exception {
    try (AdminClient adminClient =
        AdminClient.create(
            ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
      consumer =
          new KafkaConsumer<>(
              ImmutableMap.of(
                  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  bootstrapServers,
                  ConsumerConfig.GROUP_ID_CONFIG,
                  "tc-" + UUID.randomUUID(),
                  ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                  "earliest"),
              new StringDeserializer(),
              new SpanDataDeserializer());

      List<NewTopic> topics = singletonList(new NewTopic(TOPIC, 1, (short) 1));
      adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

      consumer.subscribe(singletonList(TOPIC));
    }
  }
}
