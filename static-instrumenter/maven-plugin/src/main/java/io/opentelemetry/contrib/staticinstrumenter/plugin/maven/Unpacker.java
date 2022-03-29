/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

class Unpacker {

  private final Path targetFolder;

  Unpacker(Path targetFolder) {
    this.targetFolder = targetFolder;
  }

  List<Path> copyAndUnpack(Path artifact, FrameworkSupport frameworkSupport) throws IOException {

    List<Path> result = new ArrayList<>();
    result.add(copy(artifact, frameworkSupport));
    unpack(artifact, result);
    return result;
  }

  private Path copy(Path artifact, FrameworkSupport frameworkSupport) throws IOException {

    Path targetArtifact = Files.createFile(targetFolder.resolve(artifact));

    try (ZipOutputStream targetOut = new ZipOutputStream(Files.newOutputStream(targetArtifact));
        JarFile artifactJar = new JarFile(artifact.toFile())) {

      targetOut.setMethod(ZipOutputStream.STORED);
      Enumeration<JarEntry> enums = artifactJar.entries();
      while (enums.hasMoreElements()) {
        JarEntry entry = enums.nextElement();
        frameworkSupport.copyRemovingPrefix(entry, artifactJar, targetOut);
      }
    }
    return targetArtifact;
  }

  private void unpack(Path artifact, List<Path> unpacked) throws IOException {

    try (JarFile artifactJar = new JarFile(artifact.toFile())) {
      Enumeration<JarEntry> enums = artifactJar.entries();
      while (enums.hasMoreElements()) {
        JarEntry entry = enums.nextElement();
        if (entry.getName().endsWith(".jar")) {
          unpacked.add(unpackSingle(entry, artifactJar));
        }
      }
    }
  }

  private Path unpackSingle(JarEntry entry, JarFile jarFile) throws IOException {

    Path targetFile = Files.createFile(targetFolder.resolve(entry.getName()));
    try (InputStream jarFileInputStream = jarFile.getInputStream(entry)) {
      Files.copy(jarFileInputStream, targetFile);
    }
    return targetFile;
  }
}
