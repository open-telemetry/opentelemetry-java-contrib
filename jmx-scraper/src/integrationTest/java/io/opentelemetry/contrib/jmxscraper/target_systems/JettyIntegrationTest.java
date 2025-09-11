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
import org.testcontainers.images.builder.ImageFromDockerfile;

class JettyIntegrationTest extends TargetSystemIntegrationTest {

  private static final int JETTY_PORT = 8080;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    GenericContainer<?> container =
        new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder ->
                        builder
                            .from("jetty:11")
                            .run(
                                "java",
                                "-jar",
                                "/usr/local/jetty/start.jar",
                                "--add-to-startd=jmx,stats,http")
                            .run("mkdir -p /var/lib/jetty/webapps/ROOT/")
                            .run("touch /var/lib/jetty/webapps/ROOT/index.html")
                            .build()));

    container
        .withEnv("JAVA_OPTIONS", genericJmxJvmArguments(jmxPort))
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(JETTY_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(JETTY_PORT, jmxPort));

    return container;
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("jetty");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "jetty.session.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of sessions established in total.")
                    .hasUnit("{session}")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("resource")))
        .add(
            "jetty.session.time.total",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The total time sessions have been active.")
                    .hasUnit("s")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("resource")))
        .add(
            "jetty.session.time.max",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("The maximum amount of time a session has been active.")
                    .hasUnit("s")
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("resource")))
        .add(
            "jetty.select.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of select calls.")
                    .hasUnit("{operation}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attributeWithAnyValue("context"), attributeWithAnyValue("id"))))
        .add(
            "jetty.thread.count",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("The current number of threads.")
                    .hasUnit("{thread}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("state", "busy")),
                        attributeGroup(attribute("state", "idle"))))
        .add(
            "jetty.thread.queue.count",
            metric ->
                metric
                    .isGauge()
                    .hasDescription("The current number of threads in the queue.")
                    .hasUnit("{thread}")
                    .hasDataPointsWithoutAttributes() // Got rid of id (see jetty.yaml)
            );
  }
}
