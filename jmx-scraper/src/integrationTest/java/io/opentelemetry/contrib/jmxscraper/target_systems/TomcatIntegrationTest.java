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

public class TomcatIntegrationTest extends TargetSystemIntegrationTest {

  private static final int TOMCAT_PORT = 8080;

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder ->
                        builder
                            .from("tomcat:9.0")
                            .run("rm", "-fr", "/usr/local/tomcat/webapps/ROOT")
                            .add(
                                "https://tomcat.apache.org/tomcat-9.0-doc/appdev/sample/sample.war",
                                "/usr/local/tomcat/webapps/ROOT.war")
                            .build()))
        .withEnv("CATALINA_OPTS", genericJmxJvmArguments(jmxPort))
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(TOMCAT_PORT, jmxPort)
        .waitingFor(Wait.forListeningPorts(TOMCAT_PORT, jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("tomcat");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    return MetricsVerifier.create()
        .add(
            "tomcat.sessions",
            metric ->
                metric
                    .hasDescription("The number of active sessions")
                    .hasUnit("{session}")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(attributeWithAnyValue("context")))
        .add(
            "tomcat.errors",
            metric ->
                metric
                    .hasDescription("The number of errors encountered")
                    .hasUnit("{error}")
                    .isCounter()
                    .hasDataPointsWithOneAttribute(attribute("proto_handler", "\"http-nio-8080\"")))
        .add(
            "tomcat.processing_time",
            metric ->
                metric
                    .hasDescription("The total processing time")
                    .hasUnit("ms")
                    .isCounter()
                    .hasDataPointsWithOneAttribute(attribute("proto_handler", "\"http-nio-8080\"")))
        .add(
            "tomcat.traffic",
            metric ->
                metric
                    .hasDescription("The number of bytes transmitted and received")
                    .hasUnit("By")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("direction", "sent"),
                            attribute("proto_handler", "\"http-nio-8080\"")),
                        attributeGroup(
                            attribute("direction", "received"),
                            attribute("proto_handler", "\"http-nio-8080\""))))
        .add(
            "tomcat.threads",
            metric ->
                metric
                    .hasDescription("The number of threads")
                    .hasUnit("{thread}")
                    .isGauge()
                    .hasDataPointsWithAttributes(
                        attributeGroup(
                            attribute("state", "idle"),
                            attribute("proto_handler", "\"http-nio-8080\"")),
                        attributeGroup(
                            attribute("state", "busy"),
                            attribute("proto_handler", "\"http-nio-8080\""))))
        .add(
            "tomcat.max_time",
            metric ->
                metric
                    .hasDescription("Maximum time to process a request")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithOneAttribute(attribute("proto_handler", "\"http-nio-8080\"")))
        .add(
            "tomcat.request_count",
            metric ->
                metric
                    .hasDescription("The total requests")
                    .hasUnit("{request}")
                    .isCounter()
                    .hasDataPointsWithOneAttribute(
                        attribute("proto_handler", "\"http-nio-8080\"")));
  }
}
