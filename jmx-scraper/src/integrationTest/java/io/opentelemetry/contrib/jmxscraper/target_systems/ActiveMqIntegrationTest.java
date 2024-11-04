/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGaugeWithAttributes;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class ActiveMqIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder -> builder.from("apache/activemq-classic:5.18.6").build()))
        .withEnv("JAVA_TOOL_OPTIONS", genericJmxJvmArguments(jmxPort))
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forListeningPort());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target) {
    return scraper.withTargetSystem("activemq");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.consumer.count",
                "The number of consumers currently reading from the broker.",
                "consumers",
                /* isMonotonic= */ false,
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.producer.count",
                "The number of producers currently attached to the broker.",
                "producers",
                /* isMonotonic= */ false,
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.connection.count",
                "The total number of current connections.",
                "connections",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("broker", "localhost"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.memory.usage",
                "The percentage of configured memory used.",
                "%",
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.disk.store_usage",
                "The percentage of configured disk used for persistent messages.",
                "%",
                attrs -> attrs.containsOnly(entry("broker", "localhost"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.disk.temp_usage",
                "The percentage of configured disk used for non-persistent messages.",
                "%",
                attrs -> attrs.containsOnly(entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.current",
                "The current number of messages waiting to be consumed.",
                "messages",
                /* isMonotonic= */ false,
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.expired",
                "The total number of messages not delivered because they expired.",
                "messages",
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.enqueued",
                "The total number of messages received by the broker.",
                "messages",
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.dequeued",
                "The total number of messages delivered to consumers.",
                "messages",
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.message.wait_time.avg",
                "The average time a message was held on a destination.",
                "ms",
                attrs ->
                    attrs.containsOnly(
                        entry("destination", "ActiveMQ.Advisory.MasterBroker"),
                        entry("broker", "localhost"))));
  }
}
