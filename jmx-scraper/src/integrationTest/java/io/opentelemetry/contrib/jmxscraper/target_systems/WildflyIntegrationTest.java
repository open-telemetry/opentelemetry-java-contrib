/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSum;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class WildflyIntegrationTest extends TargetSystemIntegrationTest {

  @SuppressWarnings("NonFinalStaticField")
  private static Path tempJbossClient = null;

  @AfterAll
  public static void cleanup() throws IOException {
    if (tempJbossClient != null) {
      Files.delete(tempJbossClient);
    }
  }

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    // JMX port is ignored here as we are using HTTP management interface

    String appWar = System.getProperty("app.war.path");
    Path appWarPath = Paths.get(appWar);
    assertThat(appWarPath).isNotEmptyFile().isReadable();

    return new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder ->
                        builder
                            .from("quay.io/wildfly/wildfly:32.0.1.Final-jdk11")
                            // user/pwd needed for remote JMX access
                            .run("/opt/jboss/wildfly/bin/add-user.sh user password --silent")
                            // standalone with management (HTTP) interface enabled
                            .cmd(
                                "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0")
                            .expose(8080, 9990)
                            .build()))
        .withCopyFileToContainer(
            MountableFile.forHostPath(appWarPath),
            "/opt/jboss/wildfly/standalone/deployments/testapp.war")
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forLogMessage(".*Http management interface listening on.*", 1));
  }

  @Override
  protected String scraperBaseImage() {
    // we need to run the scraper with Java 11+ because jboss client jar is using Java 11
    return "eclipse-temurin:11.0.25_9-jdk-noble";
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target) {

    if (tempJbossClient == null) {
      // copy jboss-client.jar from jboss/wildfly container
      try {
        tempJbossClient = Files.createTempFile("jboss_", "_test").toAbsolutePath();
        target.copyFileFromContainer(
            "/opt/jboss/wildfly/bin/client/jboss-client.jar", tempJbossClient.toString());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return scraper
        .withTargetSystem("wildfly")
        // Copy jboss-client.jar and add it to scraper classpath
        .withCopyFileToContainer(MountableFile.forHostPath(tempJbossClient), "/jboss-client.jar")
        .withExtraJar("/jboss-client.jar")
        // Using jboss remote HTTP protocol provided in jboss-client.jar
        .withServiceUrl("service:jmx:remote+http://targetsystem:9990")
        // Admin user created when creating container
        // When scraper is running on same host as jboss/wildfly a local file challenge can be used
        // for authentication, but here we have to use valid credentials for remote access
        .withUser("user")
        .withPassword("password");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.count",
                "The number of requests received.",
                "{request}",
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
                "{request}",
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
                "{connection}",
                attrs ->
                    attrs.containsOnly(entry("data_source", "ExampleDS"), entry("state", "active")),
                attrs ->
                    attrs.containsOnly(entry("data_source", "ExampleDS"), entry("state", "idle"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.jdbc.request.wait",
                "The number of jdbc connections that had to wait before opening.",
                "{request}",
                attrs -> attrs.containsOnly(entry("data_source", "ExampleDS"))),
        metric ->
            assertSum(
                metric,
                "wildfly.jdbc.transaction.count",
                "The number of transactions created.",
                "{transaction}"),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.jdbc.rollback.count",
                "The number of transactions rolled back.",
                "{transaction}",
                attrs -> attrs.containsOnly(entry("cause", "system")),
                attrs -> attrs.containsOnly(entry("cause", "resource")),
                attrs -> attrs.containsOnly(entry("cause", "application"))));
  }
}
