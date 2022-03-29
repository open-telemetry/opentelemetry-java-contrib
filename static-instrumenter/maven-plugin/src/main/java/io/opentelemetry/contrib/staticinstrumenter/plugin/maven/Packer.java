/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.createZipEntryFromFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

class Packer {

  private final Path targetFolder;
  private final String finalNameSuffix;

  Packer(Path targetFolder, String finalNameSuffix) {
    this.targetFolder = targetFolder;
    this.finalNameSuffix = finalNameSuffix;
  }

  Path packAndCopy(Path mainArtifact, FrameworkSupport frameworkSupport) throws IOException {

    Path mainDirectory = mainArtifact.getParent();
    if (mainDirectory == null) {
      throw new IOException("Main artifact " + mainArtifact + " needs to have a parent.");
    }
    String targetFileName = createFinalName(mainArtifact.getFileName().toString());
    Path targetFile = targetFolder.resolve(targetFileName);
    Files.createFile(targetFile);

    try (JarFile sourceJar = new JarFile(mainArtifact.toFile());
        ZipOutputStream targetOut = new ZipOutputStream(Files.newOutputStream(targetFile))) {

      targetOut.setMethod(ZipOutputStream.STORED);
      Enumeration<JarEntry> enums = sourceJar.entries();
      while (enums.hasMoreElements()) {
        JarEntry entry = enums.nextElement();
        if (entry.getName().endsWith(".jar")) {
          String[] dependencyPathElements = entry.getName().split("/");
          String dependencyName = dependencyPathElements[dependencyPathElements.length - 1];
          createZipEntryFromFile(targetOut, mainDirectory.resolve(dependencyName), entry.getName());
        } else {
          frameworkSupport.copyAddingPrefix(entry, sourceJar, targetOut);
        }
      }
    }

    return targetFile;
  }

  private String createFinalName(String instrumentedFileName) {
    int lastDotIndex = instrumentedFileName.lastIndexOf('.');
    return String.format(
        "%s%s%s",
        instrumentedFileName.substring(0, lastDotIndex),
        finalNameSuffix,
        instrumentedFileName.substring(lastDotIndex));
  }
}
