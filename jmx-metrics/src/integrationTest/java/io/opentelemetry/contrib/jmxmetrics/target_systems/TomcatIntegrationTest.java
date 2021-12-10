/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class TomcatIntegrationTest extends AbstractIntegrationTest {

  TomcatIntegrationTest() {
    super(false, "target-systems/tomcat.properties");
  }

  @Container
  GenericContainer<?> tomcat =
      new GenericContainer<>(new ImageFromDockerfile()
          .withDockerfileFromBuilder(builder ->
              builder
                  .from("tomcat:9.0.46-jdk11-openjdk-buster")
                  .add("https://tomcat.apache.org/tomcat-9.0-doc/appdev/sample/sample.war","/usr/local/tomcat/webapps/ROOT.war")
                  .build()))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("tomcat")
          .withEnv("CATALINA_OPTS", "-Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 ")
          .withExposedPorts(9010)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertGauge(
                metric,
                "cassandra.client.request.range_slice.latency.50p",
                "Token range read request latency - 50th percentile",
                "µs"));
  }
}
