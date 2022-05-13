/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.MountableFile;

abstract class KafkaIntegrationTest extends AbstractIntegrationTest {
  protected KafkaIntegrationTest(String configName) {
    super(/* configFromStdin= */ false, configName);
  }

  @Container
  GenericContainer<?> zookeeper =
      new GenericContainer<>("zookeeper:3.5")
          .withNetwork(Network.SHARED)
          .withNetworkAliases("zookeeper")
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Container
  GenericContainer<?> kafka =
      new GenericContainer<>("bitnami/kafka:2.8.1")
          .withNetwork(Network.SHARED)
          .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
          .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
          .withEnv("JMX_PORT", "7199")
          .withNetworkAliases("kafka")
          .withExposedPorts(7199)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(
              Wait.forLogMessage(".*KafkaServer.*started \\(kafka.server.KafkaServer\\).*", 1))
          .dependsOn(zookeeper);

  Startable createTopics =
      new Startable() {
        @Override
        public void start() {
          try {
            kafka.execInContainer(
                "sh",
                "-c",
                "unset JMX_PORT; for i in `seq 3`; do kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-topic-$i; done");
          } catch (IOException | InterruptedException e) {
            throw new AssertionError(e);
          }
        }

        @Override
        public void stop() {}

        @Override
        public Set<Startable> getDependencies() {
          return Collections.singleton(kafka);
        }
      };

  protected GenericContainer<?> kafkaProducerContainer() {
    return new GenericContainer<>("bitnami/kafka:latest")
        .withNetwork(Network.SHARED)
        .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
        .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
        .withEnv("JMX_PORT", "7199")
        .withNetworkAliases("kafka-producer")
        .withExposedPorts(7199)
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("target-systems/kafka-producer.sh"),
            "/usr/bin/kafka-producer.sh")
        .withCommand("kafka-producer.sh")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("kafka-producer")))
        .waitingFor(Wait.forLogMessage(".*Welcome to the Bitnami kafka container.*", 1))
        .dependsOn(createTopics);
  }

  List<Consumer<Metric>> kafkaBrokerAssertions() {
    return Arrays.asList(
        metric ->
            assertSumWithAttributes(
                metric,
                "messaging.kafka.request.time.total",
                "The total time the broker has taken to service requests",
                "ms",
                attrs -> attrs.containsOnly(entry("type", "produce")),
                attrs -> attrs.containsOnly(entry("type", "fetchfollower")),
                attrs -> attrs.containsOnly(entry("type", "fetchconsumer"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "messaging.kafka.request.time.50p",
                "The 50th percentile time the broker has taken to service requests",
                "ms",
                attrs -> attrs.containsOnly(entry("type", "produce")),
                attrs -> attrs.containsOnly(entry("type", "fetchfollower")),
                attrs -> attrs.containsOnly(entry("type", "fetchconsumer"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "messaging.kafka.request.time.99p",
                "The 99th percentile time the broker has taken to service requests",
                "ms",
                attrs -> attrs.containsOnly(entry("type", "produce")),
                attrs -> attrs.containsOnly(entry("type", "fetchfollower")),
                attrs -> attrs.containsOnly(entry("type", "fetchconsumer"))),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.partitions.all",
                "The number of partitions in the broker.",
                "{partitions}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.partitions.offline",
                "The number of offline partitions.",
                "{partitions}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.partitions.under-replicated",
                "The number of under replicated partitions.",
                "{partitions}",
                /* isMonotonic= */ false),
        metric ->
            assertSumWithAttributes(
                metric,
                "messaging.kafka.isr.operations",
                "The number of in-sync replica shrink and expand operations.",
                "{operations}",
                attrs -> attrs.containsOnly(entry("operation", "shrink")),
                attrs -> attrs.containsOnly(entry("operation", "expand"))),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.controllers.active",
                "The number of active controllers in the broker.",
                "{controllers}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.leader.elections",
                "Leader election rate (increasing values indicates broker failures).",
                "{elections}"),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.max.lag",
                "Max lag in messages between follower and leader replicas.",
                "{messages}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "messaging.kafka.leader.unclean-elections",
                "Unclean leader election rate (increasing values indicates broker failures).",
                "{elections}"));
  }

  static class KafkaBrokerTargetIntegrationTest extends KafkaIntegrationTest {
    KafkaBrokerTargetIntegrationTest() {
      super("target-systems/kafka.properties");
    }

    @Container GenericContainer<?> producer = kafkaProducerContainer();

    @Test
    void endToEnd() {
      waitAndAssertMetrics(kafkaBrokerAssertions());
    }
  }

  static class KafkaConsumerIntegrationTest extends KafkaIntegrationTest {
    KafkaConsumerIntegrationTest() {
      super("target-systems/kafka-consumer.properties");
    }

    @Container
    GenericContainer<?> consumer =
        new GenericContainer<>("bitnami/kafka:latest")
            .withNetwork(Network.SHARED)
            .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
            .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
            .withEnv("JMX_PORT", "7199")
            .withNetworkAliases("kafka-consumer")
            .withExposedPorts(7199)
            .withCommand(
                "kafka-console-consumer.sh",
                "--bootstrap-server",
                "kafka:9092",
                "--whitelist",
                "test-topic-.*",
                "--max-messages",
                "100")
            .withStartupTimeout(Duration.ofMinutes(2))
            .waitingFor(Wait.forLogMessage(".*Welcome to the Bitnami kafka container.*", 1))
            .dependsOn(createTopics);

    @Test
    void endToEnd() {
      List<String> topics = Arrays.asList("test-topic-1", "test-topic-2", "test-topic-3");
      waitAndAssertMetrics(
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.bytes-consumed-rate",
                  "The average number of bytes consumed per second",
                  "by",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.fetch-rate",
                  "The number of fetch requests for all topics per second",
                  "1"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.fetch-size-avg",
                  "The average number of bytes fetched per request",
                  "by",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.records-consumed-rate",
                  "The average number of records consumed per second",
                  "1",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.records-lag-max",
                  "Number of messages the consumer lags behind the producer",
                  "1"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.total.bytes-consumed-rate",
                  "The average number of bytes consumed for all topics per second",
                  "by"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.total.fetch-size-avg",
                  "The average number of bytes fetched per request for all topics",
                  "by"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.consumer.total.records-consumed-rate",
                  "The average number of records consumed for all topics per second",
                  "1"));
    }
  }

  static class KafkaProducerIntegrationTest extends KafkaIntegrationTest {
    KafkaProducerIntegrationTest() {
      super("target-systems/kafka-producer.properties");
    }

    @Container GenericContainer<?> producer = kafkaProducerContainer();

    @Test
    void endToEnd() {
      List<String> topics = Collections.singletonList("test-topic-1");
      waitAndAssertMetrics(
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.bytes.rate",
                  "The average number of bytes sent per second for a specific topic.",
                  "by/s",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.compression-ratio",
                  "The average compression ratio of record batches for a specific topic.",
                  "{compression}",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.io-wait-time-ns-avg",
                  "The average length of time the I/O thread spent waiting for a socket ready for reads or writes",
                  "ns"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.outgoing-bytes.rate",
                  "The average number of outgoing bytes sent per second to all servers.",
                  "by/s"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.record-error.rate",
                  "The average per-second number of record sends that resulted in errors for a specific topic.",
                  "{errors}/s",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.record-retry.rate",
                  "The average per-second number of retried record sends for a specific topic.",
                  "{retries}/s",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.record-sent.rate",
                  "The average number of records sent per second for a specific topic.",
                  "{records_sent}/s",
                  topics),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.request-latency-avg",
                  "The average request latency",
                  "ms"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.request-rate",
                  "The average number of requests sent per second",
                  "1"),
          metric ->
              assertKafkaGauge(
                  metric,
                  "messaging.kafka.producer.responses.rate",
                  "The average number of responses received per second.",
                  "{responses}/s"));
    }
  }

  static class KafkaAndJvmIntegrationText extends KafkaIntegrationTest {
    KafkaAndJvmIntegrationText() {
      super("target-systems/jvm-and-kafka.properties");
    }

    @Test
    void endToEnd() {
      List<String> gcLabels =
          Arrays.asList(
              "CodeHeap 'non-nmethods'",
              "CodeHeap 'non-profiled nmethods'",
              "CodeHeap 'profiled nmethods'",
              "Compressed Class Space",
              "G1 Eden Space",
              "G1 Old Gen",
              "G1 Survivor Space",
              "Metaspace");
      List<Consumer<Metric>> assertions = new ArrayList<>(kafkaBrokerAssertions());
      assertions.addAll(
          Arrays.asList(
              metric -> assertGauge(metric, "jvm.classes.loaded", "number of loaded classes", "1"),
              metric ->
                  assertTypedSum(
                      metric,
                      "jvm.gc.collections.count",
                      "total number of collections that have occurred",
                      "1",
                      Arrays.asList("G1 Young Generation", "G1 Old Generation")),
              metric ->
                  assertTypedSum(
                      metric,
                      "jvm.gc.collections.elapsed",
                      "the approximate accumulated collection elapsed time in milliseconds",
                      "ms",
                      Arrays.asList("G1 Young Generation", "G1 Old Generation")),
              metric ->
                  assertGauge(metric, "jvm.memory.heap.committed", "current heap usage", "by"),
              metric -> assertGauge(metric, "jvm.memory.heap.init", "current heap usage", "by"),
              metric -> assertGauge(metric, "jvm.memory.heap.max", "current heap usage", "by"),
              metric -> assertGauge(metric, "jvm.memory.heap.used", "current heap usage", "by"),
              metric ->
                  assertGauge(
                      metric, "jvm.memory.nonheap.committed", "current non-heap usage", "by"),
              metric ->
                  assertGauge(metric, "jvm.memory.nonheap.init", "current non-heap usage", "by"),
              metric ->
                  assertGauge(metric, "jvm.memory.nonheap.max", "current non-heap usage", "by"),
              metric ->
                  assertGauge(metric, "jvm.memory.nonheap.used", "current non-heap usage", "by"),
              metric ->
                  assertTypedGauge(
                      metric,
                      "jvm.memory.pool.committed",
                      "current memory pool usage",
                      "by",
                      gcLabels),
              metric ->
                  assertTypedGauge(
                      metric, "jvm.memory.pool.init", "current memory pool usage", "by", gcLabels),
              metric ->
                  assertTypedGauge(
                      metric, "jvm.memory.pool.max", "current memory pool usage", "by", gcLabels),
              metric ->
                  assertTypedGauge(
                      metric, "jvm.memory.pool.used", "current memory pool usage", "by", gcLabels),
              metric -> assertGauge(metric, "jvm.threads.count", "number of threads", "1")));

      waitAndAssertMetrics(assertions);
    }
  }

  static void assertKafkaGauge(Metric metric, String name, String description, String unit) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();

    assertThat(metric.getGauge().getDataPointsList())
        .satisfiesExactly(
            point ->
                assertThat(point.getAttributesList())
                    .singleElement()
                    .satisfies(
                        attribute -> {
                          assertThat(attribute.getKey()).isEqualTo("client-id");
                          assertThat(attribute.getValue().getStringValue()).isNotEmpty();
                        }));
  }

  @SuppressWarnings("unchecked")
  static void assertKafkaGauge(
      Metric metric, String name, String description, String unit, List<String> topics) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();

    assertThat(metric.getGauge().getDataPointsList())
        .satisfiesExactlyInAnyOrder(
            topics.stream()
                .map(
                    topic ->
                        (Consumer<NumberDataPoint>)
                            point ->
                                assertThat(point.getAttributesList())
                                    .satisfiesExactlyInAnyOrder(
                                        attribute -> {
                                          assertThat(attribute.getKey()).isEqualTo("client-id");
                                          assertThat(attribute.getValue().getStringValue())
                                              .isNotEmpty();
                                        },
                                        attribute -> {
                                          assertThat(attribute.getKey()).isEqualTo("topic");
                                          assertThat(attribute.getValue().getStringValue())
                                              .isEqualTo(topic);
                                        }))
                .toArray(Consumer[]::new));
  }
}
