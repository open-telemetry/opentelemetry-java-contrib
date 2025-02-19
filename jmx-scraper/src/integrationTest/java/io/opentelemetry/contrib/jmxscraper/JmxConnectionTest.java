/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Tests all supported ways to connect to remote JMX interface. This indirectly tests
 * JmxConnectionBuilder and relies on containers to minimize the JMX/RMI network complications which
 * are not NAT-friendly.
 */
@Disabled // failing with "container should stop when testing connection"
public class JmxConnectionTest {

  // OTLP endpoint is not used in test mode, but still has to be provided
  private static final String DUMMY_OTLP_ENDPOINT = "http://dummy-otlp-endpoint:8080/";
  private static final String SCRAPER_BASE_IMAGE = "openjdk:8u342-jre-slim";

  private static final int JMX_PORT = 9999;
  private static final String APP_HOST = "app";

  private static final Logger jmxScraperLogger = LoggerFactory.getLogger("JmxScraperContainer");
  private static final Logger appLogger = LoggerFactory.getLogger("TestAppContainer");

  private static Network network;

  @BeforeAll
  static void beforeAll() {
    network = Network.newNetwork();
  }

  @AfterAll
  static void afterAll() {
    network.close();
  }

  @Test
  void connectionError() {
    try (JmxScraperContainer scraper = scraperContainer().withRmiServiceUrl("unknown_host", 1234)) {
      scraper.start();
      waitTerminated(scraper);
      checkConnectionLogs(scraper, /* expectedOk= */ false);
    }
  }

  @Test
  void connectNoAuth() {
    connectionTest(
        app -> app.withJmxPort(JMX_PORT), scraper -> scraper.withRmiServiceUrl(APP_HOST, JMX_PORT));
  }

  @Test
  void userPassword() {
    String login = "user";
    String pwd = "t0p!Secret";
    connectionTest(
        app -> app.withJmxPort(JMX_PORT).withUserAuth(login, pwd),
        scraper -> scraper.withRmiServiceUrl(APP_HOST, JMX_PORT).withUser(login).withPassword(pwd));
  }

  private static void connectionTest(
      Function<TestAppContainer, TestAppContainer> customizeApp,
      Function<JmxScraperContainer, JmxScraperContainer> customizeScraper) {
    try (TestAppContainer app = customizeApp.apply(appContainer())) {
      app.start();
      try (JmxScraperContainer scraper = customizeScraper.apply(scraperContainer())) {
        scraper.start();
        waitTerminated(scraper);
        checkConnectionLogs(scraper, /* expectedOk= */ true);
      }
    }
  }

  private static void checkConnectionLogs(JmxScraperContainer scraper, boolean expectedOk) {

    String[] logLines = scraper.getLogs().split("\n");
    String lastLine = logLines[logLines.length - 1];

    if (expectedOk) {
      assertThat(lastLine)
          .describedAs("should log connection success")
          .endsWith("JMX connection test OK");
    } else {
      assertThat(lastLine)
          .describedAs("should log connection failure")
          .endsWith("JMX connection test ERROR");
    }
  }

  private static void waitTerminated(GenericContainer<?> container) {
    int retries = 10;
    while (retries > 0 && container.isRunning()) {
      retries--;
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    assertThat(retries)
        .describedAs("container should stop when testing connection")
        .isNotEqualTo(0);
  }

  private static JmxScraperContainer scraperContainer() {
    return new JmxScraperContainer(DUMMY_OTLP_ENDPOINT, SCRAPER_BASE_IMAGE)
        .withLogConsumer(new Slf4jLogConsumer(jmxScraperLogger))
        .withNetwork(network)
        // mandatory to have a target system even if we don't collect metrics
        .withTargetSystem("jvm")
        // we are only testing JMX connection here
        .withTestJmx();
  }

  private static TestAppContainer appContainer() {
    return new TestAppContainer()
        .withLogConsumer(new Slf4jLogConsumer(appLogger))
        .withNetwork(network)
        .withNetworkAliases(APP_HOST);
  }
}
