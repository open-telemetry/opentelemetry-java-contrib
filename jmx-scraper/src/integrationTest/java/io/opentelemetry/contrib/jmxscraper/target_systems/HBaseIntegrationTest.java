/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGauge;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGaugeWithAttributes;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSum;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.data.MapEntry.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class HBaseIntegrationTest extends TargetSystemIntegrationTest {
  private static final int DEFAULT_MASTER_SERVICE_PORT = 16000;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("dajobe/hbase")
        .withEnv("HBASE_MASTER_OPTS", genericJmxJvmArguments(jmxPort))
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(jmxPort, DEFAULT_MASTER_SERVICE_PORT)
        .waitingFor(Wait.forListeningPorts(jmxPort, DEFAULT_MASTER_SERVICE_PORT));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("hbase");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.master.region_server.count",
                "The number of region servers.",
                "{server}",
                /* isMonotonic= */ false,
                attrs -> attrs.contains(entry("state", "dead")),
                attrs -> attrs.contains(entry("state", "live"))),
        metric ->
            assertSum(
                metric,
                "hbase.master.regions_in_transition.count",
                "The number of regions that are in transition.",
                "{region}",
                /* isMonotonic= */ false),
        metric ->
            assertSum(
                metric,
                "hbase.master.regions_in_transition.over_threshold",
                "The number of regions that have been in transition longer than a threshold time.",
                "{region}",
                /* isMonotonic= */ false),
        metric ->
            assertGauge(
                metric,
                "hbase.master.regions_in_transition.oldest_age",
                "The age of the longest region in transition.",
                "ms"),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.region.count",
                "The number of regions hosted by the region server.",
                "{region}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.disk.store_file.count",
                "The number of store files on disk currently managed by the region server.",
                "{file}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.disk.store_file.size",
                "Aggregate size of the store files on disk.",
                "By",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.write_ahead_log.count",
                "The number of write ahead logs not yet archived.",
                "{log}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.request.count",
                "The number of requests received.",
                "{request}",
                /* isMonotonic= */ false,
                attrs -> {
                  attrs.contains(entry("state", "write"));
                  attrs.containsKey("region_server");
                },
                attrs -> {
                  attrs.contains(entry("state", "read"));
                  attrs.containsKey("region_server");
                }),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.queue.length",
                "The number of RPC handlers actively servicing requests.",
                "{handler}",
                /* isMonotonic= */ false,
                attrs -> {
                  attrs.contains(entry("state", "flush"));
                  attrs.containsKey("region_server");
                },
                attrs -> {
                  attrs.contains(entry("state", "compaction"));
                  attrs.containsKey("region_server");
                }),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.blocked_update.time",
                "Amount of time updates have been blocked so the memstore can be flushed.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.block_cache.operation.count",
                "Number of block cache hits/misses.",
                "{operation}",
                attrs -> {
                  attrs.contains(entry("state", "miss"));
                  attrs.containsKey("region_server");
                },
                attrs -> {
                  attrs.contains(entry("state", "hit"));
                  attrs.containsKey("region_server");
                }),
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
                "hbase.region_server.operation.append.latency.min",
                "Append operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.append.latency.mean",
                "Append operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.append.latency.median",
                "Append operation median latency.",
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
                "hbase.region_server.operation.delete.latency.min",
                "Delete operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.delete.latency.mean",
                "Delete operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.delete.latency.median",
                "Delete operation median latency.",
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
                "hbase.region_server.operation.put.latency.min",
                "Put operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.put.latency.mean",
                "Put operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.put.latency.median",
                "Put operation median latency.",
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
                "hbase.region_server.operation.get.latency.min",
                "Get operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.get.latency.mean",
                "Get operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.get.latency.median",
                "Get operation median latency.",
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
                "hbase.region_server.operation.replay.latency.min",
                "Replay operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.replay.latency.mean",
                "Replay operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.replay.latency.median",
                "Replay operation median latency.",
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
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.increment.latency.min",
                "Increment operation minimum latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.increment.latency.mean",
                "Increment operation mean latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "hbase.region_server.operation.increment.latency.median",
                "Increment operation median latency.",
                "ms",
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.operations.slow",
                "Number of operations that took over 1000ms to complete.",
                "{operation}",
                /* isMonotonic= */ false,
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
                "{connection}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.active_handler.count",
                "The number of RPC handlers actively servicing requests.",
                "{handler}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsKey("region_server")),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.queue.request.count",
                "The number of currently enqueued requests.",
                "{request}",
                /* isMonotonic= */ false,
                attrs -> attrs.contains(entry("state", "replication")),
                attrs -> attrs.contains(entry("state", "user")),
                attrs -> attrs.contains(entry("state", "priority"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hbase.region_server.authentication.count",
                "Number of client connection authentication failures/successes.",
                "{authentication request}",
                /* isMonotonic= */ false,
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
