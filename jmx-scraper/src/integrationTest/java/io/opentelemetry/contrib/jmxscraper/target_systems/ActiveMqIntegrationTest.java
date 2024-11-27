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
        .assertUpDownCounterWithAttributes(
            "activemq.consumer.count",
            "The number of consumers currently reading from the broker.",
            "{consumer}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertUpDownCounterWithAttributes(
            "activemq.producer.count",
            "The number of producers currently attached to the broker.",
            "{producer}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertUpDownCounterWithAttributes(
            "activemq.connection.count",
            "The total number of current connections.",
            "{connection}",
            entry("broker", "localhost"))
        .assertGaugeWithAttributes(
            "activemq.memory.usage",
            "The percentage of configured memory used.",
            "%",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertGaugeWithAttributes(
            "activemq.disk.store_usage",
            "The percentage of configured disk used for persistent messages.",
            "%",
            entry("broker", "localhost"))
        .assertGaugeWithAttributes(
            "activemq.disk.temp_usage",
            "The percentage of configured disk used for non-persistent messages.",
            "%",
            entry("broker", "localhost"))
        .assertUpDownCounterWithAttributes(
            "activemq.message.current",
            "The current number of messages waiting to be consumed.",
            "{message}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertCounterWithAttributes(
            "activemq.message.expired",
            "The total number of messages not delivered because they expired.",
            "{message}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertCounterWithAttributes(
            "activemq.message.enqueued",
            "The total number of messages received by the broker.",
            "{message}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertCounterWithAttributes(
            "activemq.message.dequeued",
            "The total number of messages delivered to consumers.",
            "{message}",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"))
        .assertGaugeWithAttributes(
            "activemq.message.wait_time.avg",
            "The average time a message was held on a destination.",
            "ms",
            entry("destination", "ActiveMQ.Advisory.MasterBroker"),
            entry("broker", "localhost"));
  }
}
