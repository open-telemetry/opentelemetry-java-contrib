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
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

class TomcatIntegrationTest extends AbstractIntegrationTest {

  TomcatIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/tomcat.properties");
  }

  @Container
  GenericContainer<?> tomcat =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withDockerfileFromBuilder(
                      builder ->
                          builder
                              .from("tomcat:9.0")
                              .run("rm", "-fr", "/usr/local/tomcat/webapps/ROOT")
                              .add(
                                  "https://tomcat.apache.org/tomcat-9.0-doc/appdev/sample/sample.war",
                                  "/usr/local/tomcat/webapps/ROOT.war")
                              .env(
                                  "CATALINA_OPTS",
                                  "-Dcom.sun.management.jmxremote.local.only=false "
                                      + "-Dcom.sun.management.jmxremote.authenticate=false "
                                      + "-Dcom.sun.management.jmxremote.ssl=false "
                                      + "-Dcom.sun.management.jmxremote.port=9010 "
                                      + "-Dcom.sun.management.jmxremote.rmi.port=9010")
                              .build()))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("tomcat")
          .withExposedPorts(9010)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertGauge(metric, "tomcat.sessions", "The number of active sessions.", "{session}"),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.errors",
                "The number of errors encountered.",
                "{error}",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.processing_time",
                "The total processing time.",
                "ms",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.traffic",
                "The number of bytes transmitted and received.",
                "By",
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
                "{thread}",
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
                "Maximum time to process a request.",
                "ms",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.request_count",
                "The total requests.",
                "{request}",
                attrs -> attrs.containsOnly(entry("proto_handler", "\"http-nio-8080\""))));
  }
}
