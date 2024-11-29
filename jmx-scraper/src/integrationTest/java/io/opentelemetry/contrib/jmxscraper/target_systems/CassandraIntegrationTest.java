/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGauge;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSum;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.MapAssert;
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
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.50p",
                "Token range read request latency - 50th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.99p",
                "Token range read request latency - 99th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.max",
                "Maximum token range read request latency",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.50p",
                "Standard read request latency - 50th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.99p",
                "Standard read request latency - 99th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.read.latency.max",
                "Maximum standard read request latency",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.50p",
                "Regular write request latency - 50th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.99p",
                "Regular write request latency - 99th percentile",
                "us"),
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.write.latency.max",
                "Maximum regular write request latency",
                "us"),
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
                /* isMonotonic= */ false),
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
                /* isMonotonic= */ false),
        metric ->
            assertSumWithAttributes(
                metric,
                "cassandra.client.request.count",
                "Number of requests by operation",
                "1",
                attrs -> attrs.containsOnly(entry("operation", "RangeSlice")),
                attrs -> attrs.containsOnly(entry("operation", "Read")),
                attrs -> attrs.containsOnly(entry("operation", "Write"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "cassandra.client.request.error.count",
                "Number of request errors by operation",
                "1",
                getRequestErrorCountAttributes()));
  }

  @SuppressWarnings("unchecked")
  private static Consumer<MapAssert<String, String>>[] getRequestErrorCountAttributes() {
    List<String> operations = Arrays.asList("RangeSlice", "Read", "Write");
    List<String> statuses = Arrays.asList("Timeout", "Failure", "Unavailable");

    return operations.stream()
        .flatMap(
            op ->
                statuses.stream()
                    .map(
                        st ->
                            (Consumer<MapAssert<String, String>>)
                                attrs ->
                                    attrs.containsOnly(
                                        entry("operation", op), entry("status", st))))
        .toArray(Consumer[]::new);
  }
}
