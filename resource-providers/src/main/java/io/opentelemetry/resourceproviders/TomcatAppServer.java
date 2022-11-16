/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;

class TomcatAppServer implements AppServer {

  private static final String SERVER_CLASS_NAME = "org.apache.catalina.startup.Bootstrap";
  private final ResourceLocator locator;

  TomcatAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public boolean isValidAppName(Path path) {
    if (Files.isDirectory(path)) {
      String name = path.getFileName().toString();
      return !"docs".equals(name)
          && !"examples".equals(name)
          && !"host-manager".equals(name)
          && !"manager".equals(name);
    }
    return true;
  }

  @Override
  public boolean isValidResult(Path path, @Nullable String result) {
    String name = path.getFileName().toString();
    return !"ROOT".equals(name) || !"Welcome to Tomcat".equals(result);
  }

  @Nullable
  @Override
  public Path getDeploymentDir() throws URISyntaxException {
    String catalinaBase = System.getProperty("catalina.base");
    if (catalinaBase != null) {
      return Paths.get(catalinaBase, "webapps");
    }

    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome != null) {
      return Paths.get(catalinaHome, "webapps");
    }

    // if neither catalina.base nor catalina.home is set try to deduce the location of webapps based
    // on the loaded server class.
    Class<?> serverClass = getServerClass();
    if (serverClass == null) {
      return null;
    }
    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    // jar is in bin/. First call to getParent strips jar name and the second bin/. We'll end up
    // with a path to server root to which we append the autodeploy directory.
    return getWebappsDir(jarPath);
  }

  @Nullable
  private static Path getWebappsDir(Path jarPath) {
    Path parent = jarPath.getParent();
    if (parent == null) {
      return null;
    }
    Path grandparent = parent.getParent();
    return grandparent == null ? null : grandparent.resolve("webapps");
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }

  @Override
  public boolean supportsEar() {
    return false;
  }
}
