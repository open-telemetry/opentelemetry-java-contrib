/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class HadoopIntegrationTest extends TargetSystemIntegrationTest {

  private static final int HADOOP_PORT = 50070;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("bmedora/hadoop:2.9-base")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("hadoop-env.sh", 0400),
            "/hadoop/etc/hadoop/hadoop-env.sh")
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        .withExposedPorts(HADOOP_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(HADOOP_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("hadoop");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.capacity.usage",
                "The current used capacity across all data nodes reporting to the name node.",
                "by",
                /* isMonotonic= */ false,
                attrs -> attrs.contains(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.capacity.limit",
                "The total capacity allotted to data nodes reporting to the name node.",
                "by",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.count",
                "The total number of blocks on the name node.",
                "{block}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.missing",
                "The number of blocks reported as missing to the name node.",
                "{block}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.corrupt",
                "The number of blocks reported as corrupt to the name node.",
                "{block}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.volume.failed",
                "The number of failed volumes reported to the name node.",
                "{volume}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.file.count",
                "The total number of files being tracked by the name node.",
                "{file}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.file.load",
                "The current number of concurrent file accesses.",
                "{operation}",
                /* isMonotonic= */ false,
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.data_node.count",
                "The number of data nodes reporting to the name node.",
                "{node}",
                /* isMonotonic= */ false,
                attrs ->
                    attrs.containsOnly(entry("node_name", "test-host"), entry("state", "live")),
                attrs ->
                    attrs.containsOnly(entry("node_name", "test-host"), entry("state", "dead"))));
  }
}
