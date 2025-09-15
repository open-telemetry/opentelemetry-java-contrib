/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcher;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

class WildflyIntegrationTest extends TargetSystemIntegrationTest {

  private static final int WILDFLY_SERVICE_PORT = 8080;
  private static final int WILDFLY_MANAGEMENT_PORT = 9990;

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
                            .expose(WILDFLY_SERVICE_PORT, WILDFLY_MANAGEMENT_PORT)
                            .build()))
        .withCopyFileToContainer(
            MountableFile.forHostPath(appWarPath),
            "/opt/jboss/wildfly/standalone/deployments/testapp.war")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(WILDFLY_SERVICE_PORT, WILDFLY_MANAGEMENT_PORT)
        .waitingFor(Wait.forListeningPorts(WILDFLY_SERVICE_PORT, WILDFLY_MANAGEMENT_PORT));
  }

  @Override
  protected String scraperBaseImage() {
    // we need to run the scraper with Java 11+ because jboss client jar is using Java 11
    return "eclipse-temurin:11.0.25_9-jdk-noble";
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {

    Path tempJbossClient;
    // copy jboss-client.jar from jboss/wildfly container
    try {
      tempJbossClient = Files.createTempFile(tempDir, "jboss_", "_test").toAbsolutePath();
      target.copyFileFromContainer(
          "/opt/jboss/wildfly/bin/client/jboss-client.jar", tempJbossClient.toString());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return scraper
        .withTargetSystem("wildfly")
        // Copy jboss-client.jar and add it to scraper classpath
        .withCopyFileToContainer(MountableFile.forHostPath(tempJbossClient), "/jboss-client.jar")
        .withExtraJar("/jboss-client.jar")
        // Using jboss remote HTTP protocol provided in jboss-client.jar
        .withServiceUrl("service:jmx:remote+http://targetsystem:" + WILDFLY_MANAGEMENT_PORT)
        // Admin user created when creating container
        // When scraper is running on same host as jboss/wildfly a local file challenge can be used
        // for authentication, but here we have to use valid credentials for remote access
        .withUser("user")
        .withPassword("password");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {

    AttributeMatcherGroup serverListenerAttributes =
        attributeGroup(attribute("server", "default-server"), attribute("listener", "default"));
    AttributeMatcher deploymentAttribute = attribute("deployment", "testapp.war");
    AttributeMatcher datasourceAttribute = attribute("data_source", "ExampleDS");
    return MetricsVerifier.create()
        .add(
            "wildfly.session.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of sessions created.")
                    .hasUnit("{session}")
                    .hasDataPointsWithOneAttribute(deploymentAttribute))
        .add(
            "wildfly.session.active",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of currently active sessions.")
                    .hasUnit("{session}")
                    .hasDataPointsWithOneAttribute(deploymentAttribute))
        .add(
            "wildfly.session.expired",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of sessions that have expired.")
                    .hasUnit("{session}")
                    .hasDataPointsWithOneAttribute(deploymentAttribute))
        .add(
            "wildfly.session.rejected",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of sessions that have been rejected.")
                    .hasUnit("{session}")
                    .hasDataPointsWithOneAttribute(deploymentAttribute))
        .add(
            "wildfly.request.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of requests received.")
                    .hasUnit("{request}")
                    .hasDataPointsWithAttributes(serverListenerAttributes))
        .add(
            "wildfly.request.time",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The total amount of time spent on requests.")
                    .hasUnit("ns")
                    .hasDataPointsWithAttributes(serverListenerAttributes))
        .add(
            "wildfly.request.server_error",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of requests that have resulted in a 5xx response.")
                    .hasUnit("{request}")
                    .hasDataPointsWithAttributes(serverListenerAttributes))
        .add(
            "wildfly.network.io",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of bytes transmitted.")
                    .hasUnit("By")
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("server", "default-server"),
                            attribute("listener", "default"),
                            attribute("state", "in")),
                        attributeGroup(
                            attribute("server", "default-server"),
                            attribute("listener", "default"),
                            attribute("state", "out"))))
        .add(
            "wildfly.jdbc.connection.open",
            metric ->
                metric
                    .isUpDownCounter()
                    .hasDescription("The number of open jdbc connections.")
                    .hasUnit("{connection}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(datasourceAttribute, attribute("state", "active")),
                        attributeGroup(datasourceAttribute, attribute("state", "idle"))))
        .add(
            "wildfly.jdbc.request.wait",
            metric ->
                metric
                    .isCounter()
                    .hasDescription(
                        "The number of jdbc connections that had to wait before opening.")
                    .hasUnit("{request}")
                    .hasDataPointsWithOneAttribute(datasourceAttribute))
        .add(
            "wildfly.jdbc.transaction.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of transactions created.")
                    .hasUnit("{transaction}")
                    .hasDataPointsWithoutAttributes())
        .add(
            "wildfly.jdbc.rollback.count",
            metric ->
                metric
                    .isCounter()
                    .hasDescription("The number of transactions rolled back.")
                    .hasUnit("{transaction}")
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("cause", "system")),
                        attributeGroup(attribute("cause", "resource")),
                        attributeGroup(attribute("cause", "application"))));
  }
}
