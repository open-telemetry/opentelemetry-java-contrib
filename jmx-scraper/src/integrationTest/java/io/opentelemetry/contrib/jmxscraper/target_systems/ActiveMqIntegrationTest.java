/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class ActiveMqIntegrationTest extends TargetSystemIntegrationTest {

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
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.producer.count",
            metric ->
                metric
                    .hasDescription("The number of producers currently attached to the broker.")
                    .hasUnit("{producer}")
                    .isUpDownCounter()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.connection.count",
            metric ->
                metric
                    .hasDescription("The total number of current connections.")
                    .hasUnit("{connection}")
                    .isUpDownCounter()
                    .hasDataPointsAttributes(entry("broker", "localhost")))
        .add(
            "activemq.memory.usage",
            metric ->
                metric
                    .hasDescription("The percentage of configured memory used.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.disk.store_usage",
            metric ->
                metric
                    .hasDescription(
                        "The percentage of configured disk used for persistent messages.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsAttributes(entry("broker", "localhost")))
        .add(
            "activemq.disk.temp_usage",
            metric ->
                metric
                    .hasDescription(
                        "The percentage of configured disk used for non-persistent messages.")
                    .hasUnit("%")
                    .isGauge()
                    .hasDataPointsAttributes(entry("broker", "localhost")))
        .add(
            "activemq.message.current",
            metric ->
                metric
                    .hasDescription("The current number of messages waiting to be consumed.")
                    .hasUnit("{message}")
                    .isUpDownCounter()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.message.expired",
            metric ->
                metric
                    .hasDescription(
                        "The total number of messages not delivered because they expired.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.message.enqueued",
            metric ->
                metric
                    .hasDescription("The total number of messages received by the broker.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.message.dequeued",
            metric ->
                metric
                    .hasDescription("The total number of messages delivered to consumers.")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")))
        .add(
            "activemq.message.wait_time.avg",
            metric ->
                metric
                    .hasDescription("The average time a message was held on a destination.")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsAttributes(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost")));
  }
}
