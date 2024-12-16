/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeWithAnyValue;

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
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "hbase.master.region_server.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of region servers.")
                    .hasUnit("{server}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("state", "dead")),
                        attributeGroup(attribute("state", "live"))))
        .add(
            "hbase.master.regions_in_transition.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of regions that are in transition.")
                    .hasUnit("{region}")
                    .hasDataPointsWithoutAttributes())
        .add(
            "hbase.master.regions_in_transition.over_threshold",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription(
                        "The number of regions that have been in transition longer than a threshold time.")
                    .hasUnit("{region}")
                    .hasDataPointsWithoutAttributes())
        .add(
            "hbase.master.regions_in_transition.oldest_age",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("The age of the longest region in transition.")
                    .hasUnit("ms")
                    .hasDataPointsWithoutAttributes())
        .add(
            "hbase.region_server.region.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of regions hosted by the region server.")
                    .hasUnit("{region}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.disk.store_file.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription(
                        "The number of store files on disk currently managed by the region server.")
                    .hasUnit("{file}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.disk.store_file.size",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("Aggregate size of the store files on disk.")
                    .hasUnit("By")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.write_ahead_log.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of write ahead logs not yet archived.")
                    .hasUnit("{log}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.request.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of requests received.")
                    .hasUnit("{request}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "write"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "read"), attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.queue.length",
            metric ->
                metric
                    .logAttributes()
                    .isUpDownCounter()
                    .hasDescription("The number of RPC handlers actively servicing requests.")
                    .hasUnit("{handler}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "flush"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "compaction"),
                            attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.blocked_update.time",
            metric ->
                metric
                    .isGauge()
                    .hasDescription(
                        "Amount of time updates have been blocked so the memstore can be flushed.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.block_cache.operation.count",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Number of block cache hits/misses.")
                    .hasUnit("{operation}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "miss"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "hit"), attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.files.local",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Percent of store file data that can be read from the local.")
                    .hasUnit("%")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: append ---------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.append.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Append operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.append.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Append operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.append.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Append operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.append.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Append operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.append.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Append operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: delete ---------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.delete.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Delete operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.delete.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Delete operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.delete.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Delete operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.delete.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Delete operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.delete.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Delete operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: put ---------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.put.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Put operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.put.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Put operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.put.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Put operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.put.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Put operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.put.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Put operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: get ---------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.get.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Get operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.get.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Get operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.get.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Get operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.get.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Get operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.get.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Get operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: replay ---------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.replay.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Replay operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.replay.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Replay operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.replay.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Replay operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.replay.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Replay operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.replay.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Replay operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))

        // Operation: increment -------------------------------------------------------------------
        .add(
            "hbase.region_server.operation.increment.latency.p99",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Increment operation 99th Percentile latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.increment.latency.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Increment operation max latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.increment.latency.min",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Increment operation minimum latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.increment.latency.mean",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Increment operation mean latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.operation.increment.latency.median",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("Increment operation median latency.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        // -----------------------------------------------------------------------------------

        .add(
            "hbase.region_server.operations.slow",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("Number of operations that took over 1000ms to complete.")
                    .hasUnit("{operation}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("operation", "delete"),
                            attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("operation", "append"),
                            attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("operation", "get"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("operation", "put"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("operation", "increment"),
                            attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.open_connection.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of open connections at the RPC layer.")
                    .hasUnit("{connection}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.active_handler.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of RPC handlers actively servicing requests.")
                    .hasUnit("{handler}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.queue.request.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of currently enqueued requests.")
                    .hasUnit("{request}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "replication"),
                            attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "user"), attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "priority"),
                            attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.authentication.count",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription(
                        "Number of client connection authentication failures/successes.")
                    .hasUnit("{authentication request}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "successes"),
                            attributeWithAnyValue("region_server")),
                        attributeGroup(
                            attribute("state", "failures"),
                            attributeWithAnyValue("region_server"))))
        .add(
            "hbase.region_server.gc.time",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("Time spent in garbage collection.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.gc.young_gen.time",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("Time spent in garbage collection of the young generation.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")))
        .add(
            "hbase.region_server.gc.old_gen.time",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("Time spent in garbage collection of the old generation.")
                    .hasUnit("ms")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("region_server")));
  }
}
