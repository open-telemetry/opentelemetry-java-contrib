/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

class ActiveMqIntegrationTest extends TargetSystemIntegrationTest {

  private static final int ACTIVEMQ_PORT = 61616;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder -> builder.from("apache/activemq-classic:5.18.6").build()))
        .withEnv("JAVA_TOOL_OPTIONS", genericJmxJvmArguments(jmxPort))
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(ACTIVEMQ_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(ACTIVEMQ_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("activemq");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "activemq.consumer.count",
            metric ->
                metric
                    .hasDescription("The number of consumers currently reading from the broker.")
                    .hasUnit("{consumer}")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.producer.count",
            metric ->
                metric
                    .hasDescription("The number of producers currently attached to the broker.")
                    .hasUnit("{producer}")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.connection.count",
            metric ->
                metric
                    .hasDescription("The total number of current connections.")
                    .hasUnit("{connection}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(attribute("broker", "localhost")))
        .add(
            "activemq.memory.usage",
            metric ->
                metric
                    .hasDescription("The percentage of configured memory used.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.disk.store_usage",
            metric ->
                metric
                    .hasDescription(
                        "The percentage of configured disk used for persistent messages.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(attribute("broker", "localhost")))
        .add(
            "activemq.disk.temp_usage",
            metric ->
                metric
                    .hasDescription(
                        "The percentage of configured disk used for non-persistent messages.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(attribute("broker", "localhost")))
        .add(
            "activemq.message.current",
            metric ->
                metric
                    .hasDescription("The current number of messages waiting to be consumed.")
                    .hasUnit("{message}")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.message.expired",
            metric ->
                metric
                    .hasDescription(
                        "The total number of messages not delivered because they expired.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.message.enqueued",
            metric ->
                metric
                    .hasDescription("The total number of messages received by the broker.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.message.dequeued",
            metric ->
                metric
                    .hasDescription("The total number of messages delivered to consumers.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))))
        .add(
            "activemq.message.wait_time.avg",
            metric ->
                metric
                    .hasDescription("The average time a message was held on a destination.")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("destination", "ActiveMQ.Advisory.MasterBroker"),
                            attribute("broker", "localhost"))));
  }
}
