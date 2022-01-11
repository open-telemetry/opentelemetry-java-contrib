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
import org.testcontainers.utility.MountableFile;

class ActivemqIntegrationTest extends AbstractIntegrationTest {

  ActivemqIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/activemq.properties");
  }

  @Container
  GenericContainer<?> activemq =
      new GenericContainer<>(
              new ImageFromDockerfile()
                  .withDockerfileFromBuilder(
                      builder ->
                          builder
                              .from("rmohr/activemq:5.15.9-alpine")
                              .expose(10991)
                              .env(
                                  "ACTIVEMQ_JMX_OPTS",
                                  "-Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote.port=10991 -Dcom.sun.management.jmxremote.rmi.port=10991 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false")
                              .env("ACTIVEMQ_JMX", "10991")
                              //                          .env("ACTIVEMQ_OPTS","$ACTIVEMQ_JMX_OPTS
                              // -Dhawtio.authenticationEnabled=false -Dhawtio.realm=activemq
                              // -Dhawtio.role=admins
                              // -Dhawtio.rolePrincipalClasses=org.apache.activemq.jaas.GroupPrincipal")
                              //
                              // .env("ACTIVEMQ_SUNJMX_START","-Dcom.sun.management.jmxremote")
                              .build()))
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("activemq/env", 0400), "/opt/activemq/bin/env")
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("activemq")
          .withExposedPorts(10991)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertGaugeWithAttributes(
                metric,
                "activemq.memory.usage",
                "The percentage of configured memory used.",
                "%",
                attrs -> attrs.containsOnly(entry("destination", "client_test"))));
  }
}
