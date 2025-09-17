/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcher;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

class HadoopIntegrationTest extends TargetSystemIntegrationTest {

  private static final int HADOOP_PORT = 50070;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("bmedora/hadoop:2.9-base")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("hadoop-env.sh", 0400),
            "/hadoop/etc/hadoop/hadoop-env.sh")
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
        .withExposedPorts(HADOOP_PORT, jmxPort)
        .withCreateContainerCmdModifier(cmd -> cmd.withHostName("test-host"))
        .waitingFor(Wait.forListeningPorts(HADOOP_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("hadoop");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    AttributeMatcher nodeNameAttribute = attribute("node_name", "test-host");
    return MetricsVerifier.create()
        .add(
            "hadoop.name_node.capacity.usage",
            metric ->
                metric
                    .hasDescription(
                        "The current used capacity across all data nodes reporting to the name node.")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.capacity.limit",
            metric ->
                metric
                    .hasDescription(
                        "The total capacity allotted to data nodes reporting to the name node.")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.block.count",
            metric ->
                metric
                    .hasDescription("The total number of blocks on the name node.")
                    .hasUnit("{block}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.block.missing",
            metric ->
                metric
                    .hasDescription("The number of blocks reported as missing to the name node.")
                    .hasUnit("{block}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.block.corrupt",
            metric ->
                metric
                    .hasDescription("The number of blocks reported as corrupt to the name node.")
                    .hasUnit("{block}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.volume.failed",
            metric ->
                metric
                    .hasDescription("The number of failed volumes reported to the name node.")
                    .hasUnit("{volume}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.file.count",
            metric ->
                metric
                    .hasDescription("The total number of files being tracked by the name node.")
                    .hasUnit("{file}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.file.load",
            metric ->
                metric
                    .hasDescription("The current number of concurrent file accesses.")
                    .hasUnit("{operation}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(nodeNameAttribute))
        .add(
            "hadoop.name_node.data_node.count",
            metric ->
                metric
                    .hasDescription("The number of data nodes reporting to the name node.")
                    .hasUnit("{node}")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(nodeNameAttribute, attribute("state", "live")),
                        attributeGroup(nodeNameAttribute, attribute("state", "dead"))));
  }
}
