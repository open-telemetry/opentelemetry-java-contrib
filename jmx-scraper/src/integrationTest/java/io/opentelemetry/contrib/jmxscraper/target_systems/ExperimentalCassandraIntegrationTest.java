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

/**
 * Exercises the {@code experimental-cassandra} target system, whose metric definitions are
 * inherited from the instrumentation library (source {@code instrumentation}) rather than the
 * legacy definitions embedded in jmx-scraper. This also covers the code-based compaction-progress
 * handler shipped with those definitions.
 */
class ExperimentalCassandraIntegrationTest extends TargetSystemIntegrationTest {

  private static final int CASSANDRA_PORT = 9042;
  private static final String NODETOOL_JMX_PORT = "9999";

  private static final int COMPACTION_BATCHES = 4;
  private static final int COMPACTION_ROWS_PER_BATCH = 2_500;
  private static final int COMPACTION_VALUE_BYTES = 2_048;

  private static final Logger logger =
      Logger.getLogger(ExperimentalCassandraIntegrationTest.class.getName());

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("cassandra:5.0.2")
        .withEnv(
            "JVM_EXTRA_OPTS",
            genericJmxJvmArguments(jmxPort)
                + " -Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(CASSANDRA_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(CASSANDRA_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    seedCompactionData(target);
    triggerCompaction(target);
    return scraper
        .withTargetSystemSource("instrumentation")
        .withTargetSystem("experimental-cassandra");
  }

  private static void seedCompactionData(GenericContainer<?> target) {
    try {
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

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "cassandra.compaction.tasks.completed",
            metric ->
                metric
                    .hasDescription("Number of completed compactions since server start.")
                    .hasUnit("{task}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.compaction.tasks.pending",
            metric ->
                metric
                    .hasDescription("Estimated number of compactions remaining to perform.")
                    .hasUnit("{task}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.load",
            metric ->
                metric
                    .hasDescription("Size of the on disk data size this node manages.")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.hints.count",
            metric ->
                metric
                    .hasDescription(
                        "Number of hint messages written to this node since server start.")
                    .hasUnit("{hint}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.storage.hints.in_progress",
            metric ->
                metric
                    .hasDescription("Number of hints attempting to be sent currently.")
                    .hasUnit("{hint}")
                    .isUpDownCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "cassandra.client.request.latency.p50",
            metric ->
                metric
                    .hasDescription("Request latency 50th percentile by operation.")
                    .hasUnit("s")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("cassandra.operation", "read")),
                        attributeGroup(attribute("cassandra.operation", "write")),
                        attributeGroup(attribute("cassandra.operation", "rangeslice"))))
        .add(
            "cassandra.client.request.latency.p99",
            metric ->
                metric
                    .hasDescription("Request latency 99th percentile by operation.")
                    .hasUnit("s")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("cassandra.operation", "read")),
                        attributeGroup(attribute("cassandra.operation", "write")),
                        attributeGroup(attribute("cassandra.operation", "rangeslice"))))
        .add(
            "cassandra.client.request.latency.max",
            metric ->
                metric
                    .hasDescription("Maximum request latency by operation.")
                    .hasUnit("s")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("cassandra.operation", "read")),
                        attributeGroup(attribute("cassandra.operation", "write")),
                        attributeGroup(attribute("cassandra.operation", "rangeslice"))))
        .add(
            "cassandra.client.request.count",
            metric ->
                metric
                    .hasDescription("Number of requests by operation.")
                    .hasUnit("{request}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("cassandra.operation", "rangeslice")),
                        attributeGroup(attribute("cassandra.operation", "read")),
                        attributeGroup(attribute("cassandra.operation", "write"))))
        .add(
            "cassandra.client.request.error",
            metric ->
                metric
                    .hasDescription("Number of request errors by operation.")
                    .hasUnit("{error}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        errorAttributesGroup("rangeslice", "timeout"),
                        errorAttributesGroup("rangeslice", "failure"),
                        errorAttributesGroup("rangeslice", "unavailable"),
                        errorAttributesGroup("read", "timeout"),
                        errorAttributesGroup("read", "failure"),
                        errorAttributesGroup("read", "unavailable"),
                        errorAttributesGroup("write", "timeout"),
                        errorAttributesGroup("write", "failure"),
                        errorAttributesGroup("write", "unavailable")))
        .add(
            "cassandra.compaction.progress.completed",
            metric ->
                metric
                    .hasDescription("Bytes completed for in-flight compactions.")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attributeWithAnyValue("cassandra.compaction.task_type"),
                            attribute("cassandra.keyspace", "test"),
                            attribute("cassandra.table", "data"))))
        .add(
            "cassandra.compaction.progress.size",
            metric ->
                metric
                    .hasDescription("Total bytes for in-flight compactions.")
                    .hasUnit("By")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attributeWithAnyValue("cassandra.compaction.task_type"),
                            attribute("cassandra.keyspace", "test"),
                            attribute("cassandra.table", "data"))));
  }

  private static AttributeMatcherGroup errorAttributesGroup(String operation, String status) {
    return attributeGroup(
        attribute("cassandra.operation", operation), attribute("cassandra.status", status));
  }
}
