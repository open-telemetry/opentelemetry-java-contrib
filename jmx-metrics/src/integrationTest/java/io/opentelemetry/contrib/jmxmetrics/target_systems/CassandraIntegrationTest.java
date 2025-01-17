/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

class CassandraIntegrationTest extends AbstractIntegrationTest {

  CassandraIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/cassandra.properties");
  }

  @Container GenericContainer<?> cassandra = cassandraContainer();

  @Test
  void endToEnd() {
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
                "{task}"),
        metric ->
            assertGauge(
                metric,
                "cassandra.compaction.tasks.pending",
                "Estimated number of compactions remaining to perform",
                "{task}"),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.load.count",
                "Size of the on disk data size this node manages",
                "By",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.total_hints.count",
                "Number of hint messages written to this node since [re]start",
                "{hint}"),
        metric ->
            assertSum(
                metric,
                "cassandra.storage.total_hints.in_progress.count",
                "Number of hints attempting to be sent currently",
                "{hint}",
                /* isMonotonic= */ false),
        metric ->
            assertSumWithAttributes(
                metric,
                "cassandra.client.request.count",
                "Number of requests by operation",
                "{request}",
                attrs -> attrs.containsOnly(entry("operation", "RangeSlice")),
                attrs -> attrs.containsOnly(entry("operation", "Read")),
                attrs -> attrs.containsOnly(entry("operation", "Write"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "cassandra.client.request.error.count",
                "Number of request errors by operation",
                "{error}",
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
