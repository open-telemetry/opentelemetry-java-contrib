/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
public class JmxConnectionTest {

  // OTLP endpoint is not used in test mode, but still has to be provided
  private static final String DUMMY_OTLP_ENDPOINT = "http://dummy-otlp-endpoint:8080/";
  private static final String SCRAPER_BASE_IMAGE = "openjdk:8u342-jre-slim";

  private static final int JMX_PORT = 9999;
  private static final String APP_HOST = "app";

  // key/trust stores passwords
  private static final String CLIENT_PASSWORD = "client";
  private static final String SERVER_PASSWORD = "server";

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

  @Test
  void serverSsl(@TempDir Path tempDir) {
    testServerSsl(tempDir, /* sslRmiRegistry= */ false);
  }

  @Test
  void serverSslWithSslRmiRegistry(@TempDir Path tempDir) {
    testServerSsl(tempDir, /* sslRmiRegistry= */ true);
  }

  private static void testServerSsl(Path tempDir, boolean sslRmiRegistry) {
    // two keystores:
    // server keystore with public/private key pair
    // client trust store with certificate from server

    TestKeyStore serverKeyStore =
        TestKeyStore.newKeyStore(tempDir.resolve("server.jks"), SERVER_PASSWORD);
    TestKeyStore clientTrustStore =
        TestKeyStore.newKeyStore(tempDir.resolve("client.jks"), CLIENT_PASSWORD);

    X509Certificate serverCertificate = serverKeyStore.addKeyPair();
    clientTrustStore.addTrustedCertificate(serverCertificate);

    connectionTest(
        app ->
            (sslRmiRegistry ? app.withSslRmiRegistry(4242) : app)
                .withJmxPort(JMX_PORT)
                .withJmxSsl()
                .withKeyStore(serverKeyStore),
        scraper ->
            (sslRmiRegistry ? scraper.withSslRmiRegistry() : scraper)
                .withRmiServiceUrl(APP_HOST, JMX_PORT)
                .withTrustStore(clientTrustStore));
  }

  @Test
  void serverSslClientSsl(@TempDir Path tempDir) {
    // Note: this could have been made simpler by relying on the fact that keystore could be used
    // as a trust store, but having clear split provides also some extra clarity
    //
    // 4 keystores:
    // server keystore with public/private key pair
    // server truststore with client certificate
    // client key store with public/private key pair
    // client trust store with certificate from server

    TestKeyStore serverKeyStore =
        TestKeyStore.newKeyStore(tempDir.resolve("server-keystore.jks"), SERVER_PASSWORD);
    TestKeyStore serverTrustStore =
        TestKeyStore.newKeyStore(tempDir.resolve("server-truststore.jks"), SERVER_PASSWORD);

    X509Certificate serverCertificate = serverKeyStore.addKeyPair();

    TestKeyStore clientKeyStore =
        TestKeyStore.newKeyStore(tempDir.resolve("client-keystore.jks"), CLIENT_PASSWORD);
    TestKeyStore clientTrustStore =
        TestKeyStore.newKeyStore(tempDir.resolve("client-truststore.jks"), CLIENT_PASSWORD);

    X509Certificate clientCertificate = clientKeyStore.addKeyPair();

    // adding certificates in trust stores
    clientTrustStore.addTrustedCertificate(serverCertificate);
    serverTrustStore.addTrustedCertificate(clientCertificate);

    connectionTest(
        app ->
            app.withJmxPort(JMX_PORT)
                .withJmxSsl()
                .withClientSslCertificate()
                .withKeyStore(serverKeyStore)
                .withTrustStore(serverTrustStore),
        scraper ->
            scraper
                .withRmiServiceUrl(APP_HOST, JMX_PORT)
                .withKeyStore(clientKeyStore)
                .withTrustStore(clientTrustStore));
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

    // usually only the last line can be checked, however when it fails with an exception
    // the stack trace is last in the output, so it's simpler to check all lines of log output

    assertThat(logLines)
        .anySatisfy(
            line -> {
              if (expectedOk) {
                assertThat(line)
                    .describedAs("should log connection success")
                    .contains("JMX connection test OK");
              } else {
                assertThat(line)
                    .describedAs("should log connection failure")
                    .contains("JMX connection test ERROR");
              }
            });
  }

  private static void waitTerminated(GenericContainer<?> container) {
    int retries = 10;
    while (retries > 0 && container.isRunning()) {
      retries--;
      try {
        TimeUnit.MILLISECONDS.sleep(100);
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
