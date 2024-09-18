/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxscraper.TestApp;
import io.opentelemetry.contrib.jmxscraper.TestAppContainer;
import java.io.IOException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

public class JmxRemoteClientTest {

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
  void noAuth() {
    try (TestAppContainer app = new TestAppContainer().withNetwork(network).withJmxPort(9990)) {
      app.start();
      testConnector(
          () -> JmxRemoteClient.createNew(app.getHost(), app.getMappedPort(9990)).connect());
    }
  }

  @Test
  void loginPwdAuth() {
    String login = "user";
    String pwd = "t0p!Secret";
    try (TestAppContainer app =
        new TestAppContainer().withNetwork(network).withJmxPort(9999).withUserAuth(login, pwd)) {
      app.start();
      testConnector(
          () ->
              JmxRemoteClient.createNew(app.getHost(), app.getMappedPort(9999))
                  .userCredentials(login, pwd)
                  .connect());
    }
  }

  @Test
  void serverSSL() {
    // TODO: test with SSL enabled as RMI registry seems to work differently with SSL

    // create keypair (public,private)
    // create server keystore with private key
    // configure server keystore
    //
    // create client truststore with public key
    // can we configure to use a custom truststore ???
    // connect to server
  }

  private static void testConnector(ConnectorSupplier connectorSupplier) {
    try (JMXConnector connector = connectorSupplier.get()) {
      assertThat(connector.getMBeanServerConnection())
          .isNotNull()
          .satisfies(
              connection -> {
                try {
                  ObjectName name = new ObjectName(TestApp.OBJECT_NAME);
                  Object value = connection.getAttribute(name, "IntValue");
                  assertThat(value).isEqualTo(42);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private interface ConnectorSupplier {
    JMXConnector get() throws IOException;
  }
}
