/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

class TomcatIntegrationTest extends AbstractIntegrationTest {

  TomcatIntegrationTest() {
    super(false, "target-systems/tomcat.properties");
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
                                  new HashMap<String, String>() {
                                    {
                                      put(
                                          "CATALINA_OPTS",
                                          "-Dcom.sun.management.jmxremote.local.only=false "
                                              + "-Dcom.sun.management.jmxremote.authenticate=false "
                                              + "-Dcom.sun.management.jmxremote.ssl=false "
                                              + "-Dcom.sun.management.jmxremote.port=9010 "
                                              + "-Dcom.sun.management.jmxremote.rmi.port=9010");
                                    }
                                  })
                              .build()))
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withNetworkAliases("tomcat")
          .withExposedPorts(9010)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertGauge(metric, "tomcat.sessions", "The number of active sessions.", "sessions"),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.errors",
                "The number of errors encountered.",
                "errors",
                Collections.singletonList(
                    new HashMap<String, String>() {
                      {
                        put("proto_handler", "\"http-nio-8080\"");
                      }
                    })),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.processing_time",
                "The total processing time.",
                "ms",
                Collections.singletonList(
                    new HashMap<String, String>() {
                      {
                        put("proto_handler", "\"http-nio-8080\"");
                      }
                    })),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.traffic",
                "The number of bytes transmitted and received.",
                "by",
                new ArrayList<Map<String, String>>() {
                  {
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                            put("direction", "sent");
                          }
                        });
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                            put("direction", "received");
                          }
                        });
                  }
                }),
        metric ->
            assertGaugeWithAttributes(
                metric,
                "tomcat.threads",
                "The number of threads",
                "threads",
                new ArrayList<Map<String, String>>() {
                  {
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                            put("state", "idle");
                          }
                        });
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                            put("state", "busy");
                          }
                        });
                  }
                }),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.max_time",
                "Maximum time to process a request.",
                "ms",
                new ArrayList<Map<String, String>>() {
                  {
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                          }
                        });
                  }
                }),
        metric ->
            assertSumWithAttributes(
                metric,
                "tomcat.request_count",
                "The total requests.",
                "requests",
                new ArrayList<Map<String, String>>() {
                  {
                    add(
                        new HashMap<String, String>() {
                          {
                            put("proto_handler", "\"http-nio-8080\"");
                          }
                        });
                  }
                }));
  }
}
