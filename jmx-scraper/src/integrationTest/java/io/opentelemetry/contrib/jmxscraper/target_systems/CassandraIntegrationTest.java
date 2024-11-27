/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class CassandraIntegrationTest extends TargetSystemIntegrationTest {

  private static final int CASSANDRA_PORT = 9042;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("cassandra:5.0.2")
        .withEnv(
            "JVM_EXTRA_OPTS",
            genericJmxJvmArguments(jmxPort)
                // making cassandra startup faster for single node, from ~1min to ~15s
                + " -Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(CASSANDRA_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(CASSANDRA_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("cassandra");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .assertGauge(
            "cassandra.client.request.range_slice.latency.50p",
            "Token range read request latency - 50th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.range_slice.latency.99p",
            "Token range read request latency - 99th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.range_slice.latency.max",
            "Maximum token range read request latency",
            "us")
        .assertGauge(
            "cassandra.client.request.read.latency.50p",
            "Standard read request latency - 50th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.read.latency.99p",
            "Standard read request latency - 99th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.read.latency.max",
            "Maximum standard read request latency",
            "us")
        .assertGauge(
            "cassandra.client.request.write.latency.50p",
            "Regular write request latency - 50th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.write.latency.99p",
            "Regular write request latency - 99th percentile",
            "us")
        .assertGauge(
            "cassandra.client.request.write.latency.max",
            "Maximum regular write request latency",
            "us")
        .assertCounter(
            "cassandra.compaction.tasks.completed",
            "Number of completed compactions since server [re]start",
            "1")
        .assertGauge(
            "cassandra.compaction.tasks.pending",
            "Estimated number of compactions remaining to perform",
            "1")
        .assertUpDownCounter(
            "cassandra.storage.load.count", "Size of the on disk data size this node manages", "by")
        .assertCounter(
            "cassandra.storage.total_hints.count",
            "Number of hint messages written to this node since [re]start",
            "1")
        .assertUpDownCounter(
            "cassandra.storage.total_hints.in_progress.count",
            "Number of hints attempting to be sent currently",
            "1")
        .assertCounterWithAttributes(
            "cassandra.client.request.count",
            "Number of requests by operation",
            "1",
            requestCountAttributes("RangeSlice"),
            requestCountAttributes("Read"),
            requestCountAttributes("Write"))
        .assertCounterWithAttributes(
            "cassandra.client.request.error.count",
            "Number of request errors by operation",
            "1",
            errorCountAttributes("RangeSlice", "Timeout"),
            errorCountAttributes("RangeSlice", "Failure"),
            errorCountAttributes("RangeSlice", "Unavailable"),
            errorCountAttributes("Read", "Timeout"),
            errorCountAttributes("Read", "Failure"),
            errorCountAttributes("Read", "Unavailable"),
            errorCountAttributes("Write", "Timeout"),
            errorCountAttributes("Write", "Failure"),
            errorCountAttributes("Write", "Unavailable"));
  }

  private static Map<String, String> errorCountAttributes(String operation, String status) {
    Map<String, String> map = new HashMap<>();
    map.put("operation", operation);
    map.put("status", status);
    return map;
  }

  private static Map<String, String> requestCountAttributes(String operation) {
    Map<String, String> map = new HashMap<>();
    map.put("operation", operation);
    return map;
  }
}
