/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.nio.file.Path;
import javax.annotation.Nullable;

class WebSphereAppServer implements AppServer {

  private static final String SERVER_CLASS_NAME = "com.ibm.wsspi.bootstrap.WSPreLauncher";
  private final ResourceLocator locator;

  WebSphereAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public boolean isValidAppName(Path path) {
    // query.ear is bundled with websphere
    String name = path.getFileName().toString();
    return !"query.ear".equals(name);
  }

  @Nullable
  @Override
  public Path getDeploymentDir() {
    // not used
    return null;
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }
}
