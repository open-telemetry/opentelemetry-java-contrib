/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;

final class AppServerServiceNameDetector implements ServiceNameDetector {

  private static final Logger logger =
      Logger.getLogger(AppServerServiceNameDetector.class.getName());

  private final AppServer appServer;
  private final ParseBuddy parseBuddy;
  private final DirectoryTool dirTool;

  AppServerServiceNameDetector(AppServer appServer) {
    this(appServer, new ParseBuddy(appServer), new DirectoryTool());
  }

  // Exists for testing
  AppServerServiceNameDetector(AppServer appServer, ParseBuddy parseBuddy, DirectoryTool dirTool) {
    this.appServer = appServer;
    this.parseBuddy = parseBuddy;
    this.dirTool = dirTool;
  }

  @Override
  public @Nullable String detect() throws Exception {
    if (appServer.getServerClass() == null) {
      return null;
    }

    Path deploymentDir = appServer.getDeploymentDir();
    if (deploymentDir == null) {
      return null;
    }

    if (!dirTool.isDirectory(deploymentDir)) {
      logger.log(FINE, "Deployment dir '{0}' doesn't exist.", deploymentDir);
      return null;
    }

    logger.log(FINE, "Looking for deployments in '{0}'.", deploymentDir);
    try (Stream<Path> stream = dirTool.list(deploymentDir)) {
      return stream.map(this::detectName).filter(Objects::nonNull).findFirst().orElse(null);
    }
  }

  private String detectName(Path path) {
    if (!appServer.isValidAppName(path)) {
      logger.log(FINE, "Skipping '{0}'.", path);
      return null;
    }

    logger.log(FINE, "Attempting service name detection in '{0}'.", path);
    String name = path.getFileName().toString();
    if (dirTool.isDirectory(path)) {
      return parseBuddy.handleExplodedApp(path);
    }
    if (name.endsWith(".war")) {
      return parseBuddy.handlePackagedWar(path);
    }
    if (appServer.supportsEar() && name.endsWith(".ear")) {
      return parseBuddy.handlePackagedEar(path);
    }

    return null;
  }

  // Exists for testing
  static class DirectoryTool {
    boolean isDirectory(Path path) {
      return Files.isDirectory(path);
    }

    Stream<Path> list(Path path) throws IOException {
      return Files.list(path);
    }
  }
}
