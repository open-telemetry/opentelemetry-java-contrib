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

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("bmedora/hadoop:2.9-base")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("hadoop-env.sh", 0400),
            "/hadoop/etc/hadoop/hadoop-env.sh")
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(200)))
        .withExposedPorts(jmxPort)
        .withCreateContainerCmdModifier(cmd -> cmd.withHostName("test-host"))
        .waitingFor(Wait.forListeningPort());
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
                attrs -> attrs.contains(entry("node_name", "test-host")))
    );
  }
}

