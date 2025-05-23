/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.contrib.messaging.wrappers.testing.AbstractBaseTest;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Copied from <a
 * href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/kafka/kafka-clients/kafka-clients-0.11/testing/src/main/java/io/opentelemetry/instrumentation/kafkaclients/common/v0_11/internal/KafkaClientBaseTest.java>KafkaClientBaseTest</a>.
 */
@SuppressWarnings("OtelInternalJavadoc")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class KafkaClientBaseTest extends AbstractBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(KafkaClientBaseTest.class);

  protected static final String SHARED_TOPIC = "shared.topic";

  private KafkaContainer kafka;
  protected Producer<Integer, String> producer;
  protected Consumer<Integer, String> consumer;
  private final CountDownLatch consumerReady = new CountDownLatch(1);

  public static final int partition = 0;
  public static final TopicPartition topicPartition = new TopicPartition(SHARED_TOPIC, partition);

  @BeforeAll
  void setupClass() throws ExecutionException, InterruptedException, TimeoutException {
    kafka =
        new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_HEAP_OPTS", "-Xmx256m")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.Kafka.*Server\\).*", 1))
            .withStartupTimeout(Duration.ofMinutes(1));
    kafka.start();

    // create test topic
    HashMap<String, Object> adminProps = new HashMap<>();
    adminProps.put("bootstrap.servers", kafka.getBootstrapServers());

    try (AdminClient admin = AdminClient.create(adminProps)) {
      admin
          .createTopics(Collections.singletonList(new NewTopic(SHARED_TOPIC, 1, (short) 1)))
          .all()
          .get(30, TimeUnit.SECONDS);
    }

    producer = new KafkaProducer<>(producerProps());

    consumer = new KafkaConsumer<>(consumerProps());

    consumer.subscribe(
        Collections.singletonList(SHARED_TOPIC),
        new ConsumerRebalanceListener() {
          @Override
          public void onPartitionsRevoked(Collection<TopicPartition> collection) {}

          @Override
          public void onPartitionsAssigned(Collection<TopicPartition> collection) {
            consumerReady.countDown();
          }
        });
  }

  public Map<String, Object> consumerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", 10);
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", IntegerDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    return props;
  }

  public Map<String, Object> producerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("retries", 0);
    props.put("batch.size", "16384");
    props.put("linger.ms", 1);
    props.put("buffer.memory", "33554432");
    props.put("key.serializer", IntegerSerializer.class.getName());
    props.put("value.serializer", StringSerializer.class.getName());
    return props;
  }

  @AfterAll
  void cleanupClass() {
    if (producer != null) {
      producer.close();
    }
    if (consumer != null) {
      consumer.close();
    }
    kafka.stop();
  }

  @SuppressWarnings("PreferJavaTimeOverload")
  public void awaitUntilConsumerIsReady() throws InterruptedException {
    if (consumerReady.await(0, TimeUnit.SECONDS)) {
      return;
    }
    for (int i = 0; i < 60; i++) {
      consumer.poll(Duration.ZERO);
      if (consumerReady.await(3, TimeUnit.SECONDS)) {
        break;
      }
      logger.info("Consumer has not been ready for {} time(s).", i);
    }
    if (consumerReady.getCount() != 0) {
      throw new AssertionError("Consumer wasn't assigned any partitions!");
    }
    consumer.seekToBeginning(Collections.emptyList());
  }
}
