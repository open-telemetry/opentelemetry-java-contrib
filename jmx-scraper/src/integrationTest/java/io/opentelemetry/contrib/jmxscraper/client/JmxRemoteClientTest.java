/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxscraper.TestApp;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.testcontainers.utility.MountableFile;

public class JmxRemoteClientTest {

  private static final Logger logger = LoggerFactory.getLogger(JmxRemoteClientTest.class);

  private static Network network;

  private static final List<AutoCloseable> toClose = new ArrayList<>();

  @BeforeAll
  static void beforeAll() {
    network = Network.newNetwork();
    toClose.add(network);
  }

  @AfterAll
  static void afterAll() {
    for (AutoCloseable item : toClose) {
      try {
        item.close();
      } catch (Exception e) {
        logger.warn("Error closing " + item, e);
      }
    }
  }

  @Test
  void noAuth() {
    try (AppContainer app = new AppContainer().withJmxPort(9990).start()) {
      testConnector(() -> JmxRemoteClient.createNew(app.getHost(), app.getPort()).connect());
    }
  }

  @Test
  void loginPwdAuth() {
    String login = "user";
    String pwd = "t0p!Secret";
    try (AppContainer app = new AppContainer().withJmxPort(9999).withUserAuth(login, pwd).start()) {
      testConnector(
          () ->
              JmxRemoteClient.createNew(app.getHost(), app.getPort())
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

  private static class AppContainer implements Closeable {

    private final GenericContainer<?> appContainer;
    private final Map<String, String> properties;
    private int port;
    private String login;
    private String pwd;

    private AppContainer() {
      this.properties = new HashMap<>();

      properties.put("com.sun.management.jmxremote.ssl", "false"); // TODO :

      // SSL registry : com.sun.management.jmxremote.registry.ssl
      // client side ssl auth: com.sun.management.jmxremote.ssl.need.client.auth

      String appJar = System.getProperty("app.jar.path");
      assertThat(Paths.get(appJar)).isNotEmptyFile().isReadable();

      this.appContainer =
          new GenericContainer<>("openjdk:8u272-jre-slim")
              .withCopyFileToContainer(MountableFile.forHostPath(appJar), "/app.jar")
              .withLogConsumer(new Slf4jLogConsumer(logger))
              .withNetwork(network)
              .waitingFor(
                  Wait.forLogMessage(TestApp.APP_STARTED_MSG + "\\n", 1)
                      .withStartupTimeout(Duration.ofSeconds(5)))
              .withCommand("java", "-jar", "/app.jar");
    }

    @CanIgnoreReturnValue
    public AppContainer withJmxPort(int port) {
      this.port = port;
      properties.put("com.sun.management.jmxremote.port", Integer.toString(port));
      appContainer.withExposedPorts(port);
      return this;
    }

    @CanIgnoreReturnValue
    public AppContainer withUserAuth(String login, String pwd) {
      this.login = login;
      this.pwd = pwd;
      return this;
    }

    @CanIgnoreReturnValue
    AppContainer start() {
      if (pwd == null) {
        properties.put("com.sun.management.jmxremote.authenticate", "false");
      } else {
        properties.put("com.sun.management.jmxremote.authenticate", "true");

        Path pwdFile = createPwdFile(login, pwd);
        appContainer.withCopyFileToContainer(MountableFile.forHostPath(pwdFile), "/jmx.password");
        properties.put("com.sun.management.jmxremote.password.file", "/jmx.password");

        Path accessFile = createAccessFile(login);
        appContainer.withCopyFileToContainer(MountableFile.forHostPath(accessFile), "/jmx.access");
        properties.put("com.sun.management.jmxremote.access.file", "/jmx.access");
      }

      String confArgs =
          properties.entrySet().stream()
              .map(
                  e -> {
                    String s = "-D" + e.getKey();
                    if (!e.getValue().isEmpty()) {
                      s += "=" + e.getValue();
                    }
                    return s;
                  })
              .collect(Collectors.joining(" "));

      appContainer.withEnv("JAVA_TOOL_OPTIONS", confArgs).start();

      logger.info("Test application JMX port mapped to {}:{}", getHost(), getPort());

      toClose.add(this);
      return this;
    }

    int getPort() {
      return appContainer.getMappedPort(port);
    }

    String getHost() {
      return appContainer.getHost();
    }

    @Override
    public void close() {
      if (appContainer.isRunning()) {
        appContainer.stop();
      }
    }

    private static Path createPwdFile(String login, String pwd) {
      try {
        Path path = Files.createTempFile("test", ".pwd");
        writeLine(path, String.format("%s %s", login, pwd));
        return path;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static Path createAccessFile(String login) {
      try {
        Path path = Files.createTempFile("test", ".pwd");
        writeLine(path, String.format("%s %s", login, "readwrite"));
        return path;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static void writeLine(Path path, String line) throws IOException {
      line = line + "\n";
      Files.write(path, line.getBytes(StandardCharsets.UTF_8));
    }
  }
}
