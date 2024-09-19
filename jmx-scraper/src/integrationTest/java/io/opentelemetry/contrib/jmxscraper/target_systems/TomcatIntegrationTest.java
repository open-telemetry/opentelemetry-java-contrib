/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

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
        .withEnv("LOCAL_JMX", "no")
        .withEnv(
            "CATALINA_OPTS",
            "-Dcom.sun.management.jmxremote.local.only=false"
                + " -Dcom.sun.management.jmxremote.authenticate=false"
                + " -Dcom.sun.management.jmxremote.ssl=false"
                + " -Dcom.sun.management.jmxremote.port="
                + jmxPort
                + " -Dcom.sun.management.jmxremote.rmi.port="
                + jmxPort)
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forListeningPort());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("tomcat");
  }
}
