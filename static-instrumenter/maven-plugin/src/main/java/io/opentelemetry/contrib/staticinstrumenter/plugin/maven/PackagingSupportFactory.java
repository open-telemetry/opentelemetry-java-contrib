/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackagingSupportFactory {

  private static final Map<String, Supplier<PackagingSupport>> SUPPORTED_FRAMEWORKS =
      Map.of(
          "BOOT-INF/lib/", () -> new PackagingSupport("BOOT-INF/classes/"),
          "WEB-INF/lib/", () -> new PackagingSupport("WEB-INF/classes/"));

  private PackagingSupportFactory() {}

  public static PackagingSupport packagingSupportFor(Path mainArtifact) throws IOException {
    JarFile jarFile = new JarFile(mainArtifact.toFile());
    Enumeration<JarEntry> enums = jarFile.entries();
    while (enums.hasMoreElements()) {
      JarEntry entry = enums.nextElement();
      if (entry.isDirectory() && SUPPORTED_FRAMEWORKS.containsKey(entry.getName())) {
        return SUPPORTED_FRAMEWORKS.get(entry.getName()).get();
      }
    }
    return PackagingSupport.EMPTY;
  }
}
