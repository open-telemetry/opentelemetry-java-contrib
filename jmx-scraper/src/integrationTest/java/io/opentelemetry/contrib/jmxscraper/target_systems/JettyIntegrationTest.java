/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGauge;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGaugeWithAttributes;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributesMultiplePoints;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class JettyIntegrationTest extends TargetSystemIntegrationTest {

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
        .waitingFor(Wait.forLogMessage(".*Started Server.*", 1));

    return container;
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("jetty");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "jetty.session.count",
                "The number of sessions established in total.",
                "{session}",
                attrs -> attrs.containsKey("resource")),
        metric ->
            assertSumWithAttributes(
                metric,
                "jetty.session.time.total",
                "The total time sessions have been active.",
                "s",
                attrs -> attrs.containsKey("resource")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "jetty.session.time.max",
                "The maximum amount of time a session has been active.",
                "s",
                attrs -> attrs.containsKey("resource")),
        metric ->
            assertSumWithAttributesMultiplePoints(
                metric,
                "jetty.select.count",
                "The number of select calls.",
                "{operation}",
                /* isMonotonic= */ true,
                // minor divergence from jetty.groovy with extra metrics attributes
                attrs -> attrs.containsKey("context").containsKey("id")),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "jetty.thread.count",
                "The current number of threads.",
                "{thread}",
                attrs -> attrs.containsEntry("state", "busy"),
                attrs -> attrs.containsEntry("state", "idle")),
        metric ->
            assertGauge(
                metric,
                "jetty.thread.queue.count",
                "The current number of threads in the queue.",
                "{thread}"));
  }
}
