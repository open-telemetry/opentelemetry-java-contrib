/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGaugeWithAttributes;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertSumWithAttributes;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class TomcatIntegrationTest extends TargetSystemIntegrationTest {

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
        .waitingFor(Wait.forListeningPort());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("tomcat");
  }

  @Override
  protected void verifyMetrics() {
    waitAndAssertMetrics(
        metric ->
            assertGaugeWithAttributes(
                metric,
                "tomcat.sessions",
                "The number of active sessions",
                "sessions",
                attrs -> attrs.containsKey("context")),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.errors",
                "The number of errors encountered",
                "errors",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.processing_time",
                "The total processing time",
                "ms",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.traffic",
                "The number of bytes transmitted and received",
                "by",
                attrs ->
                    attrs.containsOnly(
                        entry("proto_handler", "\"http-nio-8080\""), entry("direction", "sent")),
                attrs ->
                    attrs.containsOnly(
                        entry("proto_handler", "\"http-nio-8080\""),
                        entry("direction", "received"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "tomcat.threads",
                "The number of threads",
                "threads",
                attrs ->
                    attrs.containsOnly(
                        entry("proto_handler", "\"http-nio-8080\""), entry("state", "idle")),
                attrs ->
                    attrs.containsOnly(
                        entry("proto_handler", "\"http-nio-8080\""), entry("state", "busy"))),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "tomcat.max_time",
                "Maximum time to process a request",
                "ms",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.request_count",
                "The total requests",
                "requests",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))));
  }
}
