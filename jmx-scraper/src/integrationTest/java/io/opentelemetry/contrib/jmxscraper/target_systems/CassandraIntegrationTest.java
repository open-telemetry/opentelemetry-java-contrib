/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeWithAnyValue;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class CassandraIntegrationTest extends TargetSystemIntegrationTest {

  private static final int CASSANDRA_PORT = 9042;
  private static final String NODETOOL_JMX_PORT = "9999";
  // 4 batches × 2500 rows × 2 KB = ~20 MB; at 1 MB/s throttle compaction stays visible for ~20s,
  // well within the 60s await in verifyMetrics().
  private static final int COMPACTION_BATCHES = 4;
  private static final int COMPACTION_ROWS_PER_BATCH = 2_500;
  private static final int COMPACTION_VALUE_BYTES = 2_048;
  private static final Logger logger = Logger.getLogger(CassandraIntegrationTest.class.getName());

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
    seedCompactionData(target);
    return scraper.withTargetSystem("cassandra");
  }

  @Override
  protected void afterScraperStarted(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    triggerCompaction(target);
  }

  private static void seedCompactionData(GenericContainer<?> target) {
    try {
      // Throttle compaction to 1 MB/s so it stays active long enough for the scraper
      // to collect after it has started.
      nodetool(target, "setcompactionthroughput", "1");

      execOrThrow(
          target,
          "cqlsh",
          "-e",
          "CREATE KEYSPACE IF NOT EXISTS test"
              + " WITH replication = {'class':'SimpleStrategy','replication_factor':1};"
              + "CREATE TABLE IF NOT EXISTS test.data (id uuid PRIMARY KEY, val text)"
              + " WITH compression = {'enabled':'false'};");
      nodetool(target, "disableautocompaction", "test", "data");

      // Insert and flush separate batches to produce multiple SSTables. A manual compaction later
      // has real work to merge, and the 1 MB/s throttle keeps it visible to the scraper.
      for (int ignored = 0; ignored < COMPACTION_BATCHES; ignored++) {
        seedCompactionBatch(target);
        nodetool(target, "flush", "test", "data");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to seed Cassandra data", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to seed Cassandra data", e);
    }
  }

  private static void seedCompactionBatch(GenericContainer<?> target)
      throws IOException, InterruptedException {
    execOrThrow(
        target,
        "bash",
        "-c",
        // value is generated once and reused for all rows — same content per batch is intentional,
        // only byte volume matters for compaction to have work to do.
        "set -euo pipefail; "
            + "value=$(head -c "
            + COMPACTION_VALUE_BYTES
            + " /dev/zero | tr '\\0' x); "
            + "rm -f /tmp/cassandra-data.csv; "
            + "for i in $(seq 1 "
            + COMPACTION_ROWS_PER_BATCH
            + "); do "
            + "printf \"%s,%s\\n\" \"$(cat /proc/sys/kernel/random/uuid)\" \"$value\"; "
            + "done > /tmp/cassandra-data.csv; "
            + "cqlsh -e \"COPY test.data (id, val) FROM '/tmp/cassandra-data.csv' "
            + "WITH HEADER = false AND MINBATCHSIZE = 1 AND MAXBATCHSIZE = 2;\"");
  }

  private static void triggerCompaction(GenericContainer<?> target) {
    // Run compaction in a background thread so this method returns immediately. The scraper is
    // already running when this hook fires, so a long enough table compaction emits progress.
    Thread compactionThread =
        new Thread(
            () -> {
              try {
                nodetool(target, "compact", "--", "test", "data");
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Background compaction interrupted");
              } catch (Exception e) {
                if (target.isRunning()) {
                  logger.warning("Background compaction failed: " + e.getMessage());
                }
              }
            },
            "cassandra-compaction-trigger");
    compactionThread.setDaemon(true);
    compactionThread.start();
  }

  private static void execOrThrow(GenericContainer<?> target, String... command)
      throws IOException, InterruptedException {
    Container.ExecResult result = target.execInContainer(command);
    if (result.getExitCode() != 0) {
      throw new IllegalStateException(
          String.join(" ", command)
              + " failed with exit code "
              + result.getExitCode()
              + "\nstdout:\n"
              + result.getStdout()
              + "\nstderr:\n"
              + result.getStderr());
    }
  }

  private static void nodetool(GenericContainer<?> target, String... args)
      throws IOException, InterruptedException {
    String[] command = new String[args.length + 5];
    command[0] = "nodetool";
    command[1] = "-h";
    command[2] = "localhost";
    command[3] = "-p";
    command[4] = NODETOOL_JMX_PORT;
    System.arraycopy(args, 0, command, 5, args.length);
    execOrThrow(target, command);
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "cassandra.client.request.range_slice.latency.50p",
            metric ->
                metric
                    .hasDescription("Token range read request latency - 50th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.range_slice.latency.99p",
            metric ->
                metric
                    .hasDescription("Token range read request latency - 99th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.range_slice.latency.max",
            metric ->
                metric
                    .hasDescription("Maximum token range read request latency")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.read.latency.50p",
            metric ->
                metric
                    .hasDescription("Standard read request latency - 50th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.read.latency.99p",
            metric ->
                metric
                    .hasDescription("Standard read request latency - 99th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.read.latency.max",
            metric ->
                metric
                    .hasDescription("Maximum standard read request latency")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.write.latency.50p",
            metric ->
                metric
                    .hasDescription("Regular write request latency - 50th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.write.latency.99p",
            metric ->
                metric
                    .hasDescription("Regular write request latency - 99th percentile")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.write.latency.max",
            metric ->
                metric
                    .hasDescription("Maximum regular write request latency")
                    .hasUnit("us")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.compaction.tasks.completed",
            metric ->
                metric
                    .hasDescription("Number of completed compactions since server [re]start")
                    .hasUnit("{task}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.compaction.tasks.pending",
            metric ->
                metric
                    .hasDescription("Estimated number of compactions remaining to perform")
                    .hasUnit("{task}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.load.count",
            metric ->
                metric
                    .hasDescription("Size of the on disk data size this node manages")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.total_hints.count",
            metric ->
                metric
                    .hasDescription("Number of hint messages written to this node since [re]start")
                    .hasUnit("{hint}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.total_hints.in_progress.count",
            metric ->
                metric
                    .hasDescription("Number of hints attempting to be sent currently")
                    .hasUnit("{hint}")
                    .isUpDownCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.count",
            metric ->
                metric
                    .hasDescription("Number of requests by operation")
                    .hasUnit("{request}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("operation", "RangeSlice")),
                        attributeGroup(attribute("operation", "Read")),
                        attributeGroup(attribute("operation", "Write"))))
        .add(
            "cassandra.client.request.error.count",
            metric ->
                metric
                    .hasDescription("Number of request errors by operation")
                    .hasUnit("{error}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        errorCountAttributes("RangeSlice", "Timeout"),
                        errorCountAttributes("RangeSlice", "Failure"),
                        errorCountAttributes("RangeSlice", "Unavailable"),
                        errorCountAttributes("Read", "Timeout"),
                        errorCountAttributes("Read", "Failure"),
                        errorCountAttributes("Read", "Unavailable"),
                        errorCountAttributes("Write", "Timeout"),
                        errorCountAttributes("Write", "Failure"),
                        errorCountAttributes("Write", "Unavailable")))
        .add(
            "cassandra.compaction.progress.bytes",
            metric ->
                metric
                    .hasDescription("Bytes completed for in-flight compactions")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attributeWithAnyValue("taskType"),
                            attribute("keyspace", "test"),
                            attribute("columnfamily", "data"))))
        .add(
            "cassandra.compaction.progress.total",
            metric ->
                metric
                    .hasDescription("Total bytes for in-flight compactions")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attributeWithAnyValue("taskType"),
                            attribute("keyspace", "test"),
                            attribute("columnfamily", "data"))));
  }

  private static AttributeMatcherGroup errorCountAttributes(String operation, String status) {
    return attributeGroup(attribute("operation", operation), attribute("status", status));
  }
}
