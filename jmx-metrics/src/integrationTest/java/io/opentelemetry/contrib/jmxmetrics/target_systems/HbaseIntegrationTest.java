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

class HbaseIntegrationTest extends AbstractIntegrationTest {

  HbaseIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/hbase.properties");
  }

  @Container
  GenericContainer<?> hbase =
      new GenericContainer<>(
              new ImageFromDockerfile().withFileFromClasspath("Dockerfile", "hbase/Dockerfile"))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("hbase")
          .withExposedPorts(9900)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.master.region_server.count",
                "The number of region servers.",
                "{servers}",
                attrs -> attrs.contains(entry("state", "dead")),
                attrs -> attrs.contains(entry("state", "live"))),
        metric ->
            assertSum(
                metric,
                "hbase.master.in_transition_regions.count",
                "The number of regions that are in transition.",
                "{regions}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "hbase.master.in_transition_regions.over_threshold",
                "The number of regions that have been in transition longer than a threshold time.",
                "{regions}",
                /* isMonotonic= */ false),
        metric ->
            assertGauge(
                metric,
                "hbase.master.in_transition_regions.oldest_age",
                "The age of the longest region in transition.",
                "ms"),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.region.count",
                "The number of regions hosted by the region server.",
                "{regions}",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.disk.store_file.count",
                "The number of store files on disk currently managed by the region server.",
                "{files}",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.disk.store_file.size",
                "Aggregate size of the store files on disk.",
                "By",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.write_ahead_log.count",
                "The number of write ahead logs not yet archived.",
                "{logs}",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.request.count",
                "The number of requests received.",
                "{requests}",
                attrs -> attrs.contains(entry("state", "write")),
                attrs -> attrs.contains(entry("state", "read"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.queue.length",
                "The number of RPC handlers actively servicing requests.",
                "{handlers}",
                attrs -> attrs.contains(entry("state", "flush")),
                attrs -> attrs.contains(entry("state", "compaction"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.blocked_update.time",
                "Amount of time updates have been blocked so the memstore can be flushed.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.request.count",
                "The number of requests received.",
                "{requests}",
                attrs -> attrs.contains(entry("state", "write")),
                attrs -> attrs.contains(entry("state", "read"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.files.local",
                "Percent of store file data that can be read from the local.",
                "%",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.append.latency.p99",
                "Append operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.append.latency.max",
                "Append operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.delete.latency.p99",
                "Delete operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.delete.latency.max",
                "Delete operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.put.latency.p99",
                "Put operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.put.latency.max",
                "Put operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.get.latency.p99",
                "Get operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.get.latency.max",
                "Get operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.replay.latency.p99",
                "Replay operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.replay.latency.max",
                "Replay operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.increment.latency.p99",
                "Increment operation 99th Percentile latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.increment.latency.max",
                "Increment operation max latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.operations.slow",
                "Number of operations that took over 1000ms to complete.",
                "{operations}",
                attrs -> attrs.contains(entry("operation", "delete")),
                attrs -> attrs.contains(entry("operation", "append")),
                attrs -> attrs.contains(entry("operation", "get")),
                attrs -> attrs.contains(entry("operation", "put")),
                attrs -> attrs.contains(entry("operation", "increment"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.open_connection.count",
                "The number of open connections at the RPC layer.",
                "{connections}",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.active_handler.count",
                "The number of RPC handlers actively servicing requests.",
                "{handlers}",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.queue.request.count",
                "The number of currently enqueued requests.",
                "{requests}",
                attrs -> attrs.contains(entry("state", "replication")),
                attrs -> attrs.contains(entry("state", "user")),
                attrs -> attrs.contains(entry("state", "priority"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.authentication.count",
                "Number of client connection authentication failures/successes.",
                "{authentication requests}",
                attrs -> attrs.contains(entry("state", "successes")),
                attrs -> attrs.contains(entry("state", "failures"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.gc.time",
                "Time spent in garbage collection.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.gc.young_gen.time",
                "Time spent in garbage collection of the young generation.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.gc.old_gen.time",
                "Time spent in garbage collection of the old generation.",
                "ms",
                attrs -> attrs.containsKey("region_server")));
  }
}
