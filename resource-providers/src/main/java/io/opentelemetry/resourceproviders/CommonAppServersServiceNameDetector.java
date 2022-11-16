/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

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
    return Arrays.asList(
        detectorFor(new TomeeAppServer(locator)),
        detectorFor(new TomcatAppServer(locator)),
        detectorFor(new JettyAppServer(locator)),
        detectorFor(new LibertyAppService(locator)),
        detectorFor(new WildflyAppServer(locator)),
        detectorFor(new GlassfishAppServer(locator)),
        new WebSphereServiceNameDetector(new WebSphereAppServer(locator)));
  }

  private static AppServerServiceNameDetector detectorFor(AppServer appServer) {
    return new AppServerServiceNameDetector(appServer);
  }

  private static class ResourceLocatorImpl implements ResourceLocator {

    @Override
    @Nullable
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
