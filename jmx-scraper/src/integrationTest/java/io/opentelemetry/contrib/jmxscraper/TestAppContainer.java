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

/** Test container that allows to execute {@link TestApp} in an isolated container */
public class TestAppContainer extends GenericContainer<TestAppContainer> {

  private final Map<String, String> properties;
  private int port;
  private String login;
  private String pwd;

  public TestAppContainer() {
    super("openjdk:8u272-jre-slim");

    this.properties = new HashMap<>();

    String appJar = System.getProperty("app.jar.path");
    assertThat(Paths.get(appJar)).isNotEmptyFile().isReadable();

    this.withCopyFileToContainer(MountableFile.forHostPath(appJar), "/app.jar")
        .waitingFor(
            Wait.forLogMessage(TestApp.APP_STARTED_MSG + "\\n", 1)
                .withStartupTimeout(Duration.ofSeconds(5)))
        .withCommand("java", "-jar", "/app.jar");
  }

  @CanIgnoreReturnValue
  public TestAppContainer withJmxPort(int port) {
    this.port = port;
    properties.put("com.sun.management.jmxremote.port", Integer.toString(port));
    return this.withExposedPorts(port);
  }

  @CanIgnoreReturnValue
  public TestAppContainer withUserAuth(String login, String pwd) {
    this.login = login;
    this.pwd = pwd;
    return this;
  }

  @Override
  public void start() {

    // TODO: add support for ssl
    properties.put("com.sun.management.jmxremote.ssl", "false");

    if (pwd == null) {
      properties.put("com.sun.management.jmxremote.authenticate", "false");
    } else {
      properties.put("com.sun.management.jmxremote.authenticate", "true");

      Path pwdFile = createPwdFile(login, pwd);
      this.withCopyFileToContainer(MountableFile.forHostPath(pwdFile), "/jmx.password");
      properties.put("com.sun.management.jmxremote.password.file", "/jmx.password");

      Path accessFile = createAccessFile(login);
      this.withCopyFileToContainer(MountableFile.forHostPath(accessFile), "/jmx.access");
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

    this.withEnv("JAVA_TOOL_OPTIONS", confArgs);

    logger().info("Test application JAVA_TOOL_OPTIONS = " + confArgs);

    super.start();

    logger().info("Test application JMX port mapped to {}:{}", getHost(), getMappedPort(port));
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
