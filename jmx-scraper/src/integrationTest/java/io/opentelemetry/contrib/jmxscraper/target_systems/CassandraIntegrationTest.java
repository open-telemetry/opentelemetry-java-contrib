/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class CassandraIntegrationTest extends TargetSystemIntegrationTest {

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
                        errorCountAttributes("Write", "Unavailable")));
  }

  private static AttributeMatcherGroup errorCountAttributes(String operation, String status) {
    return attributeGroup(attribute("operation", operation), attribute("status", status));
  }
}
