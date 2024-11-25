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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

class HadoopIntegrationTest extends AbstractIntegrationTest {

  HadoopIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/hadoop.properties");
  }

  @Container
  GenericContainer<?> hadoop =
      new GenericContainer<>("bmedora/hadoop:2.9-base")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("hadoop/hadoop-env.sh", 0400),
              "/hadoop/etc/hadoop/hadoop-env.sh")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("hadoop/yarn-site.xml", 0400),
              "/hadoop/etc/hadoop/yarn-site.xml")
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
                "hadoop.name_node.capacity.usage",
                "The current used capacity across all data nodes reporting to the name node.",
                "by",
                attrs -> attrs.contains(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.capacity.limit",
                "The total capacity allotted to data nodes reporting to the name node.",
                "by",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.count",
                "The total number of blocks on the name node.",
                "{block}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.missing",
                "The number of blocks reported as missing to the name node.",
                "{block}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.block.corrupt",
                "The number of blocks reported as corrupt to the name node.",
                "{block}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.volume.failed",
                "The number of failed volumes reported to the name node.",
                "{volume}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.file.count",
                "The total number of files being tracked by the name node.",
                "{file}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.file.load",
                "The current number of concurrent file accesses.",
                "{operation}",
                attrs -> attrs.containsOnly(entry("node_name", "test-host"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "hadoop.name_node.data_node.count",
                "The number of data nodes reporting to the name node.",
                "{node}",
                attrs ->
                    attrs.containsOnly(entry("node_name", "test-host"), entry("state", "live")),
                attrs ->
                    attrs.containsOnly(entry("node_name", "test-host"), entry("state", "dead"))));
  }
}
