/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarSupport.consumeEntries;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.moveEntry;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

class PackagingSupport {

  static final PackagingSupport EMPTY = new PackagingSupport("");

  private final String classesPrefix;
  private final Set<String> filesToRepackage = new HashSet<>();

  PackagingSupport(String classesPrefix) {
    this.classesPrefix = classesPrefix;
  }

  String getClassesPrefix() {
    return classesPrefix;
  }

  void copyRemovingPrefix(JarFile inputJar, ZipOutputStream targetOut) throws IOException {

    consumeEntries(
        inputJar,
        (entry) -> {
          if (!entry.isDirectory() && entry.getName().startsWith(classesPrefix)) {
            String newEntryPath = entry.getName().replace(getClassesPrefix(), "");
            moveEntry(targetOut, newEntryPath, entry, inputJar);
            filesToRepackage.add(newEntryPath);
          } else {
            moveEntry(targetOut, entry.getName(), entry, inputJar);
          }
        });
  }

  void copyAddingPrefix(JarEntry entry, JarFile inputJar, ZipOutputStream targetOut)
      throws IOException {

    if (!entry.isDirectory() && filesToRepackage.contains(entry.getName())) {
      String newEntryPath = classesPrefix + entry.getName();
      moveEntry(targetOut, newEntryPath, entry, inputJar);
    } else {
      moveEntry(targetOut, entry.getName(), entry, inputJar);
    }
  }
}
