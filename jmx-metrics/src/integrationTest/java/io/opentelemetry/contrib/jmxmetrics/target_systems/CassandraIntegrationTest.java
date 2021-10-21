/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.max",
                "Maximum token range read request latency",
                "µs"),
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
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.max",
                "Maximum standard read request latency",
                "µs"),
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
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.max",
                "Maximum regular write request latency",
                "µs"),
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
                "by",
                false),
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
                "1",
                false),
        metric ->
            assertSumWithAttributes(
                metric,
                "cassandra.client.request.count",
                "Number of requests",
                "1",
                getRequestCountAttributes()
            ));
  }

  private List<Map<String, String>> getRequestCountAttributes() {
    List<String> operations = Arrays.asList("RangeSlice", "Read", "Write");
    List<String> statuses = Arrays.asList("All", "Timeout", "Failure", "Unavailable");

    return operations.stream()
          .flatMap(op -> statuses.stream().map(st ->
              new HashMap<String, String>() {{
                  put("operation", op);
                  put("status", st);
              }}
          )).collect(Collectors.toList());
  }
}
