/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import java.time.Duration;

import static org.assertj.core.api.Assertions.entry;

class HadoopIntegrationTest extends AbstractIntegrationTest {

  HadoopIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/hadoop.properties");
  }

  @Container
  GenericContainer<?> hadoop =
      new GenericContainer<>(new ImageFromDockerfile()
          .withFileFromClasspath("hadoop-env.sh", "hadoop/hadoop-env.sh")
          .withFileFromClasspath("yarn-site.xml", "hadoop/yarn-site.xml")
          .withFileFromClasspath("Dockerfile", "hadoop/Dockerfile"))
          .withNetwork(Network.SHARED)
          .withNetworkAliases("hadoop")
          .withExposedPorts(8004)
          .withStartupTimeout(Duration.ofMinutes(2))
          .withCreateContainerCmdModifier(cmd -> cmd.withHostName("test-host"))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.disk.usage",
                "The amount of disk used by data nodes.",
                "by",
                attrs -> attrs.contains(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.disk.limit",
                "The total disk allotted to data nodes.",
                "by",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.block.count",
                "The total number of blocks.",
                "blocks",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.block.missing",
                "The number of blocks reported as missing.",
                "blocks",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.block.corrupt",
                "The number of blocks reported as corrupt.",
                "blocks",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.volume.failed",
                "The number of failed volumes.",
                "volumes",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.file.count",
                "The total number of files being tracked by the name node.",
                "files",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.hdfs.file.load",
                "The current number of concurrent file accesses.",
                "operations",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.data_node.count",
                "The number of data nodes tracked by the name node.",
                "nodes",
                attrs ->
                    attrs.containsOnly(
                        entry("node_name", "test-host"), entry("state", "live")),
                attrs ->
                    attrs.containsOnly(
                        entry("node_name", "test-host"), entry("state", "dead")))
    );
  }
}
