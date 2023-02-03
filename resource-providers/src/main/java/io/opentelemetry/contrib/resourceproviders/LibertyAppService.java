/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static java.util.logging.Level.FINE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class LibertyAppService implements AppServer {

  private static final String SERVICE_CLASS_NAME = "com.ibm.ws.kernel.boot.cmdline.EnvCheck";

  private static final Logger logger = Logger.getLogger(LibertyAppService.class.getName());
  private final ResourceLocator locator;

  LibertyAppService(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public Path getDeploymentDir() {
    // default installation has
    // WLP_OUTPUT_DIR - libertyDir/usr/servers
    // WLP_USER_DIR - libertyDir/usr
    // docker image has
    // WLP_USER_DIR - /opt/ol/wlp/usr
    // WLP_OUTPUT_DIR - /opt/ol/wlp/output

    // liberty server sets current directory to $WLP_OUTPUT_DIR/serverName we need
    // $WLP_USER_DIR/servers/serverName
    // in default installation we already have the right directory and don't need to do anything
    Path serverDir = Paths.get("").toAbsolutePath();
    String wlpUserDir = System.getenv("WLP_USER_DIR");
    String wlpOutputDir = System.getenv("WLP_OUTPUT_DIR");
    if (logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "Using WLP_USER_DIR {0}, WLP_OUTPUT_DIR {1}.",
          new Object[] {wlpUserDir, wlpOutputDir});
    }
    if (wlpUserDir != null
        && wlpOutputDir != null
        && !Paths.get(wlpOutputDir).equals(Paths.get(wlpUserDir, "servers"))) {
      Path serverName = serverDir.getFileName();
      serverDir = Paths.get(wlpUserDir, "servers").resolve(serverName);
    }

    // besides dropins applications can also be deployed via server.xml using <webApplication>,
    // <enterpriseApplication> and <application> tags, see
    // https://openliberty.io/docs/latest/reference/config/server-configuration-overview.html
    return serverDir.resolve("dropins");
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVICE_CLASS_NAME);
  }
}
