/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static java.util.logging.Level.FINE;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class WildflyAppServer implements AppServer {

  private static final Logger logger = Logger.getLogger(WildflyAppServer.class.getName());
  private static final String SERVICE_CLASS_NAME = "org.jboss.modules.Main";

  private final ResourceLocator locator;

  WildflyAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Nullable
  @Override
  public Path getDeploymentDir() throws URISyntaxException {
    String programArguments = System.getProperty("sun.java.command");
    logger.log(FINE, "Started with arguments {0}.", programArguments);
    if (programArguments == null) {
      return null;
    }
    if (!programArguments.contains("org.jboss.as.standalone")) {
      // only standalone mode is supported
      return null;
    }

    // base dir is also passed as program argument (not vm argument) -Djboss.server.base.dir we use
    // environment variable JBOSS_BASE_DIR to avoid parsing program arguments
    String jbossBaseDir = System.getenv("JBOSS_BASE_DIR");
    if (jbossBaseDir != null) {
      logger.log(FINE, "Using JBOSS_BASE_DIR {0}.", jbossBaseDir);
      return Paths.get(jbossBaseDir, "deployments");
    }

    Class<?> serverClass = getServerClass();
    if (serverClass == null) {
      return null;
    }
    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    Path parent = jarPath.getParent();
    if (parent == null) {
      return null;
    }
    return parent.resolve("standalone/deployments");
  }

  @Override
  public boolean isValidAppName(Path path) {
    // For exploded applications we should be checking for the presence of ".dodeploy" or
    // ".deployed" marker files to ensure that we are scanning only applications that are actually
    // deployed, see
    // https://access.redhat.com/documentation/en-us/jboss_enterprise_application_platform/6/html/administration_and_configuration_guide/reference_for_deployment_scanner_marker_files1
    return AppServer.super.isValidAppName(path);
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVICE_CLASS_NAME);
  }
}
