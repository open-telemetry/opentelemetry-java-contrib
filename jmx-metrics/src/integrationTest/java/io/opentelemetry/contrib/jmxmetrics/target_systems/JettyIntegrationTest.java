/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

class JettyIntegrationTest extends AbstractIntegrationTest {

  JettyIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/jetty.properties");
  }

  @Container
  GenericContainer<?> jetty =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withDockerfileFromBuilder(
                      builder ->
                          builder
                              .from("jetty")
                              .run(
                                  "java",
                                  "-jar",
                                  "/usr/local/jetty/start.jar",
                                  "--add-to-startd=jmx,stats,http")
                              .env(
                                  "JAVA_OPTIONS",
                                  "-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1099")
                              .build()))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("jetty")
          .withExposedPorts(1099)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() throws IOException {
    waitAndAssertMetrics(
        // Load on the server is required to test session metrics
        //        metric ->
        //            assertSum(
        //                metric,
        //                "jetty.session.count",
        //                "The number of sessions established.",
        //                "{sessions}"),
        //        metric ->
        //            assertSum(
        //                metric,
        //                "jetty.session.time.total",
        //                "The total time sessions have been active.",
        //                "s",
        //                /* isMonotonic= */false),
        //        metric ->
        //            assertGauge(
        //                metric,
        //                "jetty.session.time.max",
        //                "The maximum amount of time a session has been active.",
        //                "s"),
        metric ->
            assertSum(metric, "jetty.select.count", "The number of select calls.", "{operations}"),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "jetty.thread.count",
                "The current number of threads.",
                "{threads}",
                attrs -> attrs.contains(entry("state", "busy")),
                attrs -> attrs.contains(entry("state", "idle"))),
        metric ->
            assertGauge(
                metric,
                "jetty.thread.queue.count",
                "The current number of threads in the queue.",
                "{threads}"));
  }
}
