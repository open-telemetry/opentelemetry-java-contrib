/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

class CassandraIntegrationTest extends AbstractIntegrationTest {

  CassandraIntegrationTest() {
    super(false, "target-systems/cassandra.properties");
  }

  @Container
  GenericContainer<?> cassandra =
      new GenericContainer<>("cassandra:3.11")
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("cassandra/jmxremote.password", 0400),
              "/etc/cassandra/jmxremote.password")
          .withNetworkAliases("cassandra")
          .withExposedPorts(7199)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.50p",
                "Token range read request latency - 50th percentile",
                "µs"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.99p",
                "Token range read request latency - 99th percentile",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.range_slice.latency.count",
                "Number of token range read request operations",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.max",
                "Maximum token range read request latency",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.range_slice.timeout.count",
                "Number of token range read request timeouts encountered",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.range_slice.unavailable.count",
                "Number of token range read request unavailable exceptions encountered",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.50p",
                "Standard read request latency - 50th percentile",
                "µs"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.99p",
                "Standard read request latency - 99th percentile",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.read.latency.count",
                "Number of standard read request operations",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.max",
                "Maximum standard read request latency",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.read.timeout.count",
                "Number of standard read request timeouts encountered",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.read.unavailable.count",
                "Number of standard read request unavailable exceptions encountered",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.50p",
                "Regular write request latency - 50th percentile",
                "µs"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.99p",
                "Regular write request latency - 99th percentile",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.write.latency.count",
                "Number of regular write request operations",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.max",
                "Maximum regular write request latency",
                "µs"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.write.timeout.count",
                "Number of regular write request timeouts encountered",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.client.request.write.unavailable.count",
                "Number of regular write request unavailable exceptions encountered",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.compaction.tasks.completed",
                "Number of completed compactions since server [re]start",
                "1"),
        metric ->
            assertGauge(
                metric,
                "cassandra.compaction.tasks.pending",
                "Estimated number of compactions remaining to perform",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.load.count",
                "Size of the on disk data size this node manages",
                "by"),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.total_hints.count",
                "Number of hint messages written to this node since [re]start",
                "1"),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.total_hints.in_progress.count",
                "Number of hints attempting to be sent currently",
                "1"));
  }
}
