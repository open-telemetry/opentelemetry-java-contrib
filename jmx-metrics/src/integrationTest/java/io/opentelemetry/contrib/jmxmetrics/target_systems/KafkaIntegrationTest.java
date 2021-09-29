package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startable;

abstract class KafkaIntegrationTest extends AbstractIntegrationTest {
  protected KafkaIntegrationTest(String configName) {
    super(false, configName);
  }

  @Container
  GenericContainer<?> zookeeper =
      new GenericContainer<>("zookeeper:3.5")
          .withNetwork(Network.SHARED)
          .withNetworkAliases("zookeeper")
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Container
  GenericContainer<?> kafka =
      new GenericContainer<>("bitnami/kafka:latest")
          .withNetwork(Network.SHARED)
          .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
          .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
          .withEnv("JMX_PORT", "7199")
          .withNetworkAliases("kafka")
          .withExposedPorts(7199)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort())
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
          } catch (Exception e) {
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

  static class KafkaBrokerTargetIntegrationTest extends KafkaIntegrationTest {
    KafkaBrokerTargetIntegrationTest() {
      super("target-systems/kafka.properties");
    }

    @Test
    void endToEnd() {
      waitAndAssertMetrics(
          metric -> assertGauge(metric, "kafka.bytes.in", "bytes in per second from clients", "by"),
          metric -> assertGauge(metric, "kafka.bytes.out", "bytes out per second to clients", "by"),
          metric ->
              assertGauge(
                  metric, "kafka.controller.active.count", "controller is active on broker", "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.fetch.consumer.total.time.99p",
                  "fetch consumer request time - 99th percentile",
                  "ms"),
          metric ->
              assertSum(
                  metric, "kafka.fetch.consumer.total.time.count", "fetch consumer request count"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.fetch.consumer.total.time.median",
                  "fetch consumer request time - 50th percentile",
                  "ms"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.fetch.follower.total.time.99p",
                  "fetch follower request time - 99th percentile",
                  "ms"),
          metric ->
              assertSum(
                  metric, "kafka.fetch.follower.total.time.count", "fetch follower request count"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.fetch.follower.total.time.median",
                  "fetch follower request time - 50th percentile",
                  "ms"),
          metric ->
              assertGauge(metric, "kafka.isr.shrinks", "in-sync replica shrinks per second", "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.leader.election.rate",
                  "leader election rate - non-zero indicates broker failures",
                  "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.max.lag",
                  "max lag in messages between follower and leader replicas",
                  "1"),
          metric ->
              assertGauge(metric, "kafka.messages.in", "number of messages in per second", "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.partitions.offline.count",
                  "number of partitions without an active leader",
                  "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.partitions.underreplicated.count",
                  "number of under replicated partitions",
                  "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.produce.total.time.99p",
                  "produce request time - 99th percentile",
                  "ms"),
          metric -> assertSum(metric, "kafka.produce.total.time.count", "produce request count"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.produce.total.time.median",
                  "produce request time - 50th percentile",
                  "ms"),
          metric -> assertGauge(metric, "kafka.request.queue", "size of the request queue", "1"),
          metric ->
              assertGauge(
                  metric,
                  "kafka.unclean.election.rate",
                  "unclean leader election rate - non-zero indicates broker failures",
                  "1"));
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
            .withStartupTimeout(Duration.ofSeconds(120))
            .waitingFor(Wait.forListeningPort())
            .dependsOn(createTopics);

    @Test
    void endToEnd() {
      waitAndAssertMetrics(
          metric ->
              assertKafkaGauge(
                  metric,
                  "kafka.consumer.fetch-rate",
                  "The number of fetch requests for all topics per second",
                  "1"));
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
}
