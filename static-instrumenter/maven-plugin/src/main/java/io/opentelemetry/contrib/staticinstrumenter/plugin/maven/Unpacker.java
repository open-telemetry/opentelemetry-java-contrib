/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarSupport.consumeEntries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

class Unpacker {

  private final Path targetFolder;

  Unpacker(Path targetFolder) {
    this.targetFolder = targetFolder;
  }

  /**
   * Copies the artifact to the target folder, removing class prefix using packagingSupport. If
   * there are any nested JAR files, they will be unpacked into the target folder.
   *
   * @param artifact artifact to be copied and unpacked
   * @param packagingSupport relevant packaging support service
   * @return list of JARs in target directory
   * @throws IOException in case of any file operation problems
   */
  List<Path> copyAndUnpack(Path artifact, PackagingSupport packagingSupport) throws IOException {
    List<Path> result = new ArrayList<>();
    result.add(copy(artifact, packagingSupport));
    unpack(artifact, result);
    return result;
  }

  private Path copy(Path artifact, PackagingSupport packagingSupport) throws IOException {
    Path targetArtifact = Files.createFile(targetFolder.resolve(artifact.getFileName()));
    try (ZipOutputStream targetOut = new ZipOutputStream(Files.newOutputStream(targetArtifact));
        JarFile artifactJar = new JarFile(artifact.toFile())) {
      packagingSupport.copyRemovingPrefix(artifactJar, targetOut);
    }
    return targetArtifact;
  }

  private void unpack(Path artifact, List<Path> unpacked) throws IOException {
    try (JarFile artifactJar = new JarFile(artifact.toFile())) {
      consumeEntries(
          artifactJar,
          (entry) -> {
            if (entry.getName().endsWith(".jar")) {
              unpacked.add(unpackSingle(entry, artifactJar));
            }
          });
    }
  }

  private Path unpackSingle(JarEntry entry, JarFile jarFile) throws IOException {
    Path targetFile = targetFolder.resolve(entry.getName());
    // ensure parent path is created
    Files.createDirectories(targetFile.getParent());
    try (InputStream jarFileInputStream = jarFile.getInputStream(entry)) {
      Files.copy(jarFileInputStream, targetFile);
    }
    return targetFile;
  }
}
