/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static java.util.logging.Level.FINE;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class JettyAppServer implements AppServer {

  private static final Logger logger = Logger.getLogger(JettyAppServer.class.getName());
  private static final String SERVER_CLASS_NAME = "org.eclipse.jetty.start.Main";
  private final ResourceLocator locator;

  JettyAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public boolean isValidAppName(Path path) {
    // jetty deployer ignores directories ending with ".d"
    if (Files.isDirectory(path)) {
      return !path.getFileName().toString().endsWith(".d");
    }
    return true;
  }

  @Override
  public Path getDeploymentDir() {
    // Jetty expects the webapps directory to be in the directory where jetty was started from.
    // Alternatively the location of webapps directory can be specified by providing jetty base
    // directory as an argument to jetty e.g. java -jar start.jar jetty.base=/dir where webapps
    // would be located in /dir/webapps.

    String programArguments = System.getProperty("sun.java.command");
    logger.log(FINE, "Started with arguments {0}.", programArguments);
    if (programArguments != null) {
      Path jettyBase = parseJettyBase(programArguments);
      if (jettyBase != null) {
        logger.log(FINE, "Using jetty.base {0}.", jettyBase);
        return jettyBase.resolve("webapps");
      }
    }

    return Paths.get("webapps").toAbsolutePath();
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }

  @Nullable
  @VisibleForTesting
  static Path parseJettyBase(String programArguments) {
    if (programArguments == null) {
      return null;
    }
    int start = programArguments.indexOf("jetty.base=");
    if (start == -1) {
      return null;
    }
    start += "jetty.base=".length();
    if (start == programArguments.length()) {
      return null;
    }
    // Take the path until the first space. If the path doesn't exist extend it up to the next
    // space. Repeat until a path that exists is found or input runs out.
    int next = start;
    while (true) {
      int nextSpace = programArguments.indexOf(' ', next);
      if (nextSpace == -1) {
        Path candidate = Paths.get(programArguments.substring(start));
        return Files.exists(candidate) ? candidate : null;
      }
      Path candidate = Paths.get(programArguments.substring(start, nextSpace));
      next = nextSpace + 1;
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
  }

  @Override
  public boolean supportsEar() {
    return false;
  }
}
