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
import org.testcontainers.utility.MountableFile;

class WildflyIntegrationTest extends AbstractIntegrationTest {

  WildflyIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/wildfly.properties");
  }

  /* In order to create a JMX connection to WildFLy, the scraper requires an additional
  client jar in the classpath. To facilitate this, the scraper runs in the same container,
  which gives it access to the client jar packaged with WildFly. */
  @Override
  protected GenericContainer<?> buildScraper(String otlpEndpoint) {
    String scraperJarPath = System.getProperty("shadow.jar.path");

    return new GenericContainer<>("jboss/wildfly:23.0.1.Final")
        .withNetwork(Network.SHARED)
        .withNetworkAliases("wildfly")
        .withCopyFileToContainer(
            MountableFile.forHostPath(scraperJarPath), "/app/OpenTelemetryJMXMetrics.jar")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("script.groovy"), "/app/script.groovy")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("target-systems/wildfly.properties"),
            "/app/target-systems/wildfly.properties")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("wildfly/start.sh", 0007), "/app/start.sh")
        .withEnv("OTLP_ENDPOINT", otlpEndpoint)
        .withCommand("/app/start.sh")
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forLogMessage(".*Started GroovyRunner.*", 1));
  }

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.count",
                "The number of requests received.",
                "{requests}",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "default"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.time",
                "The total amount of time spent on requests.",
                "ns",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "default"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.server_error",
                "The number of requests that have resulted in a 5xx response.",
                "{requests}",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "default"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.network.io",
                "The number of bytes transmitted.",
                "by",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"),
                        entry("listener", "default"),
                        entry("state", "in")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"),
                        entry("listener", "default"),
                        entry("state", "out"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.jdbc.connection.open",
                "The number of open jdbc connections.",
                "{connections}",
                attrs ->
                    attrs.containsOnly(entry("data_source", "ExampleDS"), entry("state", "active")),
                attrs ->
                    attrs.containsOnly(entry("data_source", "ExampleDS"), entry("state", "idle"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.jdbc.request.wait",
                "The number of jdbc connections that had to wait before opening.",
                "{requests}",
                attrs -> attrs.containsOnly(entry("data_source", "ExampleDS"))),
        metric ->
            assertSum(
                metric,
                "wildfly.jdbc.transaction.count",
                "The number of transactions created.",
                "{transactions}"),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.jdbc.rollback.count",
                "The number of transactions rolled back.",
                "{transactions}",
                attrs -> attrs.containsOnly(entry("cause", "system")),
                attrs -> attrs.containsOnly(entry("cause", "resource")),
                attrs -> attrs.containsOnly(entry("cause", "application"))));
  }
}
