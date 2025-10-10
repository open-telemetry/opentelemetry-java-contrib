/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.testcontainers.utility.MountableFile;

/** Test container that allows to execute TestApp in an isolated container */
public class TestAppContainer extends GenericContainer<TestAppContainer> {

  private final Map<String, String> properties;
  private String login;
  private String pwd;
  private boolean jmxSsl;
  private boolean jmxSslRegistry;
  private TestKeyStore keyStore;
  private TestKeyStore trustStore;
  private int jmxPort;
  private int jmxRmiPort;
  private boolean clientCertificate;

  public TestAppContainer() {
    super("openjdk:8u342-jre-slim");

    this.properties = new HashMap<>();

    String appJar = System.getProperty("app.jar.path");
    assertThat(Paths.get(appJar)).isNotEmptyFile().isReadable();

    this.withCopyFileToContainer(MountableFile.forHostPath(appJar), "/app.jar")
        .waitingFor(
            Wait.forLogMessage("app started\\n", 1).withStartupTimeout(Duration.ofSeconds(5)))
        .withCommand("java", "-jar", "/app.jar");
  }

  /**
   * Configures app container for container-to-container access
   *
   * @param port mapped port to use
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withJmxPort(int port) {
    this.jmxPort = port;
    return this;
  }

  /**
   * Enables and configure JMX login/pwd authentication
   *
   * @param login user login
   * @param pwd user password
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withUserAuth(String login, String pwd) {
    this.login = login;
    this.pwd = pwd;
    return this;
  }

  /**
   * Enables SSL for JMX endpoint, will require JMX client to trust remote JVM certificate
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withJmxSsl() {
    this.jmxSsl = true;
    return this;
  }

  /**
   * Enables SSL-protected RMI registry, which requires a distinct port from JMX
   *
   * @param registryPort registry port
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withSslRmiRegistry(int registryPort) {
    this.jmxSslRegistry = true;
    this.jmxRmiPort = registryPort;
    return this;
  }

  /**
   * Enables client certificate verification by the remote JVM
   *
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withClientSslCertificate() {
    this.clientCertificate = true;
    return this;
  }

  /**
   * Configure key store for the remote JVM
   *
   * @param keyStore key store
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withKeyStore(TestKeyStore keyStore) {
    this.keyStore = keyStore;
    return this;
  }

  /**
   * Configure trust store for the remote JVM
   *
   * @param trustStore trust store
   * @return this
   */
  @CanIgnoreReturnValue
  public TestAppContainer withTrustStore(TestKeyStore trustStore) {
    this.trustStore = trustStore;
    return this;
  }

  @Override
  public void start() {
    properties.put("com.sun.management.jmxremote.port", Integer.toString(jmxPort));

    properties.put("com.sun.management.jmxremote.ssl", Boolean.toString(jmxSsl));
    if (jmxSslRegistry) {
      properties.put("com.sun.management.jmxremote.registry.ssl", "true");
      properties.put("com.sun.management.jmxremote.rmi.port", Integer.toString(jmxRmiPort));
      if (jmxRmiPort == jmxPort) {
        // making it harder to attempt using the same port
        // doing so results in a "port busy" error which can be confusing
        throw new IllegalStateException(
            "RMI with SSL registry requires a distinct port from JMX: " + jmxRmiPort);
      }
    }
    if (jmxSsl && clientCertificate) {
      properties.put("com.sun.management.jmxremote.ssl.need.client.auth", "true");
    }

    if (pwd == null) {
      // no authentication
      properties.put("com.sun.management.jmxremote.authenticate", "false");
    } else {
      // authentication enabled
      properties.put("com.sun.management.jmxremote.authenticate", "true");

      Path pwdFile = createPwdFile(login, pwd);
      this.withCopyFileToContainer(MountableFile.forHostPath(pwdFile), "/jmx.password");
      properties.put("com.sun.management.jmxremote.password.file", "/jmx.password");

      Path accessFile = createAccessFile(login);
      this.withCopyFileToContainer(MountableFile.forHostPath(accessFile), "/jmx.access");
      properties.put("com.sun.management.jmxremote.access.file", "/jmx.access");
    }

    // add optional key and trust stores
    addSecureStore(keyStore, /* isKeyStore= */ true, properties);
    addSecureStore(trustStore, /* isKeyStore= */ false, properties);

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

    this.withEnv("JAVA_TOOL_OPTIONS", confArgs);

    logger().info("Test application JAVA_TOOL_OPTIONS = {}", confArgs);

    super.start();
  }

  private void addSecureStore(
      TestKeyStore keyStore, boolean isKeyStore, Map<String, String> properties) {
    if (keyStore == null) {
      return;
    }
    Path path = keyStore.getPath();
    String containerPath = "/" + path.getFileName().toString();
    this.withCopyFileToContainer(MountableFile.forHostPath(path), containerPath);

    String prefix = String.format("javax.net.ssl.%sStore", isKeyStore ? "key" : "trust");
    properties.put(prefix, containerPath);
    properties.put(prefix + "Password", keyStore.getPassword());
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
