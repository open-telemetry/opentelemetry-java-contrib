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

class WildflyIntegrationTest extends AbstractIntegrationTest {

  WildflyIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/wildfly.properties");
  }

  @Container
  GenericContainer<?> wildfly =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withFileFromClasspath("Dockerfile", "wildfly/Dockerfile")
                  .withFileFromClasspath("start.sh", "wildfly/start.sh"))
          .withNetwork(Network.SHARED)
          .withNetworkAliases("wildfly")
          .withExposedPorts(9990)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

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
                        entry("server", "default-server"), entry("listener", "default")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "https"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.time",
                "The total amount of time spent on requests.",
                "ns",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "default")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "https"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "wildfly.request.server_error",
                "The number of requests that have resulted in a 500 response.",
                "{requests}",
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "default")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"), entry("listener", "https"))),
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
                        entry("state", "out")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"),
                        entry("listener", "https"),
                        entry("state", "in")),
                attrs ->
                    attrs.containsOnly(
                        entry("server", "default-server"),
                        entry("listener", "https"),
                        entry("state", "out"))));
  }
}
