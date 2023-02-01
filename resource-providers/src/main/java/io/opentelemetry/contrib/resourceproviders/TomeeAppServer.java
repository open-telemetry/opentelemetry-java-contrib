/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;

class TomeeAppServer implements AppServer {

  private static final String SERVER_CLASS_NAME = "org.apache.catalina.startup.Bootstrap";
  private final ResourceLocator locator;

  TomeeAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Nullable
  @Override
  public Path getDeploymentDir() throws URISyntaxException {
    Path rootDir = getRootDir();
    if (rootDir == null) {
      return null;
    }

    // check for presence of tomee configuration file, if it doesn't exist then we have tomcat not
    // tomee
    if (!Files.isRegularFile(rootDir.resolve("conf/tomee.xml"))) {
      return null;
    }

    // tomee deployment directory is configurable, we'll only look at the default 'apps' directory
    // to get the actual deployment directory (or see whether it is enabled at all) we would need to
    // parse conf/tomee.xml
    // tomee also deploys applications from webapps directory, detecting them is handled by
    // TomcatServiceNameDetector
    return rootDir.resolve("apps");
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }

  @Nullable
  private Path getRootDir() throws URISyntaxException {
    String catalinaBase = System.getProperty("catalina.base");
    if (catalinaBase != null) {
      return Paths.get(catalinaBase);
    }

    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome != null) {
      return Paths.get(catalinaHome);
    }

    // if neither catalina.base nor catalina.home is set try to deduce the location of based on the
    // loaded server class.
    Class<?> serverClass = getServerClass();
    if (serverClass == null) {
      return null;
    }
    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    // jar is in bin/. First call to getParent strips jar name and the second bin/. We'll end up
    // with a path to server root.
    Path parent = jarPath.getParent();
    if (parent == null) {
      return null;
    }
    return parent.getParent();
  }
}
