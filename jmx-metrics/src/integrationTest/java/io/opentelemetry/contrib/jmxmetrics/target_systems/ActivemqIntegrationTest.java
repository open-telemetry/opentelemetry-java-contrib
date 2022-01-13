/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

class ActivemqIntegrationTest extends AbstractIntegrationTest {

  ActivemqIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/activemq.properties");
  }

  @Container
  GenericContainer<?> activemq =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withFileFromClasspath("config/env", "activemq/config/env")
                  .withFileFromClasspath("Dockerfile", "activemq/Dockerfile"))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("activemq")
          .withExposedPorts(10991)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.consumer.count",
                "The number of consumers currently reading from the broker.",
                "{consumers}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.producer.count",
                "The number of producers currently attached to the broker.",
                "{producers}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSum(
                metric,
                "activemq.connection.count",
                "The total number of current connections.",
                "{connections}",
                /* isMonotonic= */ false),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.memory.usage",
                "The percentage of configured memory used.",
                "%",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertGauge(
                metric,
                "activemq.disk.store_usage",
                "The percentage of configured disk used for persistent messages.",
                "%"),
        metric ->
            assertGauge(
                metric,
                "activemq.disk.temp_usage",
                "The percentage of configured disk used for non-persistent messages.",
                "%"),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.current",
                "The current number of messages waiting to be consumed.",
                "{messages}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.current",
                "The current number of messages waiting to be consumed.",
                "{messages}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.expired",
                "The total number of messages not delivered because they expired.",
                "{messages}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.enqueued",
                "The total number of messages received by the broker.",
                "{messages}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "activemq.message.dequeued",
                "The total number of messages delivered to consumers.",
                "{messages}",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.message.wait_time.avg",
                "The average time a message was held on a destination.",
                "ms",
                attrs ->
                    attrs.containsOnly(entry("destination", "ActiveMQ.Advisory.MasterBroker"))));
  }
}
