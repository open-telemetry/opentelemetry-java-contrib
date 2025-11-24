/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems.kafka;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeWithAnyValue;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createKafkaConsumerContainer;
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

class KafkaConsumerIntegrationTest extends TargetSystemIntegrationTest {

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

    GenericContainer<?> kafkaProducer =
        createKafkaProducerContainer()
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("kafka-producer")))
            .withNetworkAliases("kafka-producer")
            .dependsOn(kafka);

    return Arrays.asList(zookeeper, kafka, kafkaProducer);
  }

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return createKafkaConsumerContainer()
        .withEnv("JMX_PORT", Integer.toString(jmxPort))
        .withExposedPorts(jmxPort)
        .waitingFor(Wait.forListeningPorts(jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("kafka-consumer");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    // TODO: change to follow semconv
    AttributeMatcher clientIdAttribute = attributeWithAnyValue("client-id");
    AttributeMatcher topicAttribute = attribute("topic", "test-topic-1");

    return MetricsVerifier.create()
        .add(
            "kafka.consumer.fetch-rate",
            metric ->
                metric
                    .hasDescription("The number of fetch requests for all topics per second")
                    .hasUnit("{request}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.consumer.total.bytes-consumed-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average number of bytes consumed for all topics per second")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.consumer.total.fetch-size-avg",
            metric ->
                metric
                    .hasDescription(
                        "The average number of bytes fetched per request for all topics")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.consumer.total.records-consumed-rate",
            metric ->
                metric
                    .hasDescription(
                        "The average number of records consumed for all topics per second")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.consumer.records-lag-max",
            metric ->
                metric
                    .hasDescription("Number of messages the consumer lags behind the producer")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(clientIdAttribute))
        .add(
            "kafka.consumer.bytes-consumed-rate",
            metric ->
                metric
                    .hasDescription("The average number of bytes consumed per second")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.consumer.fetch-size-avg",
            metric ->
                metric
                    .hasDescription("The average number of bytes fetched per request")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(attributeGroup(clientIdAttribute, topicAttribute)))
        .add(
            "kafka.consumer.records-consumed-rate",
            metric ->
                metric
                    .hasDescription("The average number of records consumed per second")
                    .hasUnit("{record}")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(clientIdAttribute, topicAttribute)));
  }
}
