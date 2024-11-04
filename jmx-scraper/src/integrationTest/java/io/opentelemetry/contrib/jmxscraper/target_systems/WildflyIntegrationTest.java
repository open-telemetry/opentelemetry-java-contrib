/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class WildflyIntegrationTest extends TargetSystemIntegrationTest {

  @SuppressWarnings("NonFinalStaticField")
  private static Path tempJbossClient = null;

  @AfterAll
  public static void cleanup() throws IOException {
    if (tempJbossClient != null) {
      Files.delete(tempJbossClient);
    }
  }

  // /opt/jboss/wildfly/standalone/tmp/auth/

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    // JMX port is ignored here as we are using HTTP management interface

    return new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(
                    builder ->
                        builder
                            .from("jboss/wildfly:23.0.1.Final")
                            // user/pwd needed for remote JMX access
                            .run("/opt/jboss/wildfly/bin/add-user.sh user password --silent")
                            // standalone with management (HTTP) interface enabled
                            .cmd(
                                "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0")
                            .expose(8080, 9990)
                            .build()))
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forLogMessage(".*Http management interface listening on.*", 1));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target) {

    if (tempJbossClient == null) {
      // copy jboss-client.jar from jboss/wildfly container
      try {
        tempJbossClient = Files.createTempFile("jboss_", "_test").toAbsolutePath();
        target.copyFileFromContainer(
            "/opt/jboss/wildfly/bin/client/jboss-client.jar", tempJbossClient.toString());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return scraper
        .withTargetSystem("wildfly")
        // copy jboss-client.jar and add it to scraper classpath
        .withCopyFileToContainer(MountableFile.forHostPath(tempJbossClient), "/jboss-client.jar")
        .withExtraJar("/jboss-client.jar")
        // using jboss remote HTTP protocol provided in jboss-client.jar
        .withServiceUrl("service:jmx:remote+http://targetsystem:9990")
        // admin user created when creating container
        .withUser("user")
        .withPassword("password");
  }

  @Override
  protected void verifyMetrics() {}
}
