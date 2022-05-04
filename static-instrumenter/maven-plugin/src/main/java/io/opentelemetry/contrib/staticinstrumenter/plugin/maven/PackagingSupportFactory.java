/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackagingSupportFactory {

  private static final Set<String> SUPPORTED_FRAMEWORKS =
      Set.of("BOOT-INF/classes/", "WEB-INF/classes/");

  private PackagingSupportFactory() {}

  public static PackagingSupport packagingSupportFor(Path mainArtifact) throws IOException {

    try (JarFile jarFile = new JarFile(mainArtifact.toFile())) {
      for (String frameworkKey : SUPPORTED_FRAMEWORKS) {
        JarEntry jarEntry = jarFile.getJarEntry(frameworkKey);
        if ((jarEntry != null) && jarEntry.isDirectory()) {
          return new PackagingSupport(frameworkKey);
        }
      }
    }
    return PackagingSupport.EMPTY;
  }
}
