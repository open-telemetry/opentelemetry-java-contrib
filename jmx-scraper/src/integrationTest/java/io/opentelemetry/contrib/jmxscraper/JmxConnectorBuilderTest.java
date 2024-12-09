/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

public class JmxConnectorBuilderTest {

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
    int port = PortSelector.getAvailableRandomPort();
    try (TestAppContainer app = new TestAppContainer().withHostAccessFixedJmxPort(port)) {
      app.start();
      testConnector(
          () -> JmxConnectorBuilder.createNew(app.getHost(), app.getMappedPort(port)).build());
    }
  }

  @Test
  void loginPwdAuth() {
    int port = PortSelector.getAvailableRandomPort();
    String login = "user";
    String pwd = "t0p!Secret";
    try (TestAppContainer app =
        new TestAppContainer().withHostAccessFixedJmxPort(port).withUserAuth(login, pwd)) {
      app.start();
      testConnector(
          () ->
              JmxConnectorBuilder.createNew(app.getHost(), app.getMappedPort(port))
                  .withUser(login)
                  .withPassword(pwd)
                  .build());
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
                  ObjectName name = new ObjectName("io.opentelemetry.test:name=TestApp");
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
