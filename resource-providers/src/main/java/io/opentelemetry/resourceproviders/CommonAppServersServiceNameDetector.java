/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * This class is just a factory that provides a ServiceNameDetector that knows how to find and parse
 * the most common application server configuration files.
 */
final class CommonAppServersServiceNameDetector {

  static ServiceNameDetector create() {
    return new DelegatingServiceNameDetector(detectors());
  }

  private CommonAppServersServiceNameDetector() {}

  private static List<ServiceNameDetector> detectors() {
    ResourceLocator locator = new ResourceLocatorImpl();
    // Additional implementations will be added to this list.
    return Collections.singletonList(detectorFor(new GlassfishAppServer(locator)));
  }

  private static AppServerServiceNameDetector detectorFor(AppServer appServer) {
    return new AppServerServiceNameDetector(appServer);
  }

  private static class ResourceLocatorImpl implements ResourceLocator {

    @Override
    public Class<?> findClass(String className) {
      try {
        return Class.forName(className, false, ClassLoader.getSystemClassLoader());
      } catch (ClassNotFoundException | LinkageError exception) {
        return null;
      }
    }

    @Override
    public URL getClassLocation(Class<?> clazz) {
      return clazz.getProtectionDomain().getCodeSource().getLocation();
    }
  }
}
