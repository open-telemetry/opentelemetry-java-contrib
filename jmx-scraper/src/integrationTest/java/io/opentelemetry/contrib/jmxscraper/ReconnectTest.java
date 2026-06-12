/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxscraper.target_systems.BaseTargetSystemIntegrationTest;
import io.opentelemetry.contrib.jmxscraper.target_systems.MetricsVerifier;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class ReconnectTest extends BaseTargetSystemIntegrationTest {

  @Test
  void testReconnect(@TempDir Path tmpDir) throws Exception {
    startContainers(tmpDir);
    verifyMetrics();
    target.stop();
    otlpServer.reset();
    Thread.sleep(1_000);
    List<ExportMetricsServiceRequest> receivedMetrics = otlpServer.getMetrics();
    assertThat(receivedMetrics).isEmpty();
    target.start();
    verifyMetrics();
  }

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    // reusing test application for JVM metrics and custom yaml
    //noinspection resource
    return new TestAppContainer()
        .withJmxPort(jmxPort)
        .withExposedPorts(jmxPort)
        .waitingFor(Wait.forListeningPorts(jmxPort));
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        // custom metric in custom-metrics.yaml
        .add(
            "custom.jvm.uptime",
            metric ->
                metric
                    .hasDescription("JVM uptime in milliseconds")
                    .hasUnit("ms")
                    .isCounter()
                    .hasDataPointsWithoutAttributes());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withCustomYaml("custom-metrics.yaml");
  }
}
