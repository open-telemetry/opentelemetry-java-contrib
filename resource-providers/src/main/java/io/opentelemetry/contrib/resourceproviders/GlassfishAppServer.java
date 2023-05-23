/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;

class GlassfishAppServer implements AppServer {

  private static final String SERVICE_CLASS_NAME = "com.sun.enterprise.glassfish.bootstrap.ASMain";
  private final ResourceLocator locator;

  GlassfishAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Nullable
  @Override
  public Path getDeploymentDir() {
    String instanceRoot = System.getProperty("com.sun.aas.instanceRoot");
    if (instanceRoot == null) {
      return null;
    }

    // besides autodeploy directory it is possible to deploy applications through admin console and
    // asadmin script, to detect those we would need to parse config/domain.xml
    return Paths.get(instanceRoot, "autodeploy");
  }

  @Override
  @Nullable
  public Class<?> getServerClass() {
    return locator.findClass(SERVICE_CLASS_NAME);
  }
}
