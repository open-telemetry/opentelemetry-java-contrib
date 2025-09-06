/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems.kafka;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeWithAnyValue;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createKafkaContainer;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createKafkaProducerContainer;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createZookeeperContainer;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcher;
import io.opentelemetry.contrib.jmxscraper.target_systems.MetricsVerifier;
import io.opentelemetry.contrib.jmxscraper.target_systems.TargetSystemIntegrationTest;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

class KafkaProducerIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected Collection<GenericContainer<?>> createPrerequisiteContainers() {
    GenericContainer<?> zookeeper =
        createZookeeperContainer()
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("zookeeper")))
            .withNetworkAliases("zookeeper");

    GenericContainer<?> kafka =
        createKafkaContainer()
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("kafka")))
            .withNetworkAliases("kafka")
            .dependsOn(zookeeper);

    return Arrays.asList(zookeeper, kafka);
  }

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return createKafkaProducerContainer()
        .withEnv("JMX_PORT", Integer.toString(jmxPort))
        .withExposedPorts(jmxPort)
        .waitingFor(Wait.forListeningPorts(jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("kafka-producer");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    // TODO: change to follow semconv
    AttributeMatcher clientIdAttribute = attributeWithAnyValue("client-id");
    AttributeMatcher topicAttribute = attribute("topic", "test-topic-1");

    return MetricsVerifier.create()
        .add(
            "kafka.producer.io-wait-time-ns-avg",
            metric ->
                metric
                    .hasDescription(
                        "The average length of time the I/O thread spent waiting for a socket ready for reads or writes")
                    .hasUnit("ns")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.producer.outgoing-byte-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average number of outgoing bytes sent per second to all servers")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.producer.request-latency-avg",
            metric ->
                metric
                    .hasDescription("The average request latency")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.producer.request-rate",
            metric ->
                metric
                    .hasDescription("The average number of requests sent per second")
                    .hasUnit("{request}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.producer.response-rate",
            metric ->
                metric
                    .hasDescription("Responses received per second")
                    .hasUnit("{response}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))

        // Per topic metrics
        .add(
            "kafka.producer.byte-rate",
            metric ->
                metric
                    .hasDescription("The average number of bytes sent per second for a topic")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.producer.compression-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average compression rate of record batches for a topic, defined as the average ratio of the compressed batch size divided by the uncompressed size")
                    .hasUnit("{ratio}")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.producer.record-error-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average per-second number of record sends that resulted in errors for a topic")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.producer.record-retry-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average per-second number of retried record sends for a topic")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.producer.record-send-rate",
            metric ->
                metric
                    .hasDescription("The average number of records sent per second for a topic")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(clientIdAttribute, topicAttribute)));
  }
}
