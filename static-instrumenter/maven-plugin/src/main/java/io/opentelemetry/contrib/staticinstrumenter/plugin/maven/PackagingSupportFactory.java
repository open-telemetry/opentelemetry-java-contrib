/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackagingSupportFactory {

  private static final Map<String, Supplier<PackagingSupport>> SUPPORTED_FRAMEWORKS =
      Map.of(
          "BOOT-INF/classes/", () -> new PackagingSupport("BOOT-INF/classes/"),
          "WEB-INF/classes/", () -> new PackagingSupport("WEB-INF/classes/"));

  private PackagingSupportFactory() {}

  public static PackagingSupport packagingSupportFor(Path mainArtifact) throws IOException {

    try (JarFile jarFile = new JarFile(mainArtifact.toFile())) {
      for (Map.Entry<String, Supplier<PackagingSupport>> entry : SUPPORTED_FRAMEWORKS.entrySet()) {
        JarEntry jarEntry = jarFile.getJarEntry(entry.getKey());
        if ((jarEntry != null) && jarEntry.isDirectory()) {
          return entry.getValue().get();
        }
      }
    }
    return PackagingSupport.EMPTY;
  }
}
