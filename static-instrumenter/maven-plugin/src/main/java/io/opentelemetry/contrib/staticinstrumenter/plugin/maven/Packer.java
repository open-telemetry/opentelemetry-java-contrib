/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarSupport.consumeEntries;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.createZipEntryFromFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Packer {

  private static final Logger log = LoggerFactory.getLogger(Packer.class);

  private final Path targetFolder;
  private final String finalNameSuffix;

  Packer(Path targetFolder, String finalNameSuffix) {
    this.targetFolder = targetFolder;
    this.finalNameSuffix = finalNameSuffix;
  }

  /**
   * Packs copies any nested JARs of the main artifacts from the its folder. Then, copies the main
   * artifacts to the target folder, adding entries prefixes using the packaging support service.
   * Copied artifact will carry the finalNameSuffix.
   *
   * @param mainArtifact the artifact to be packed/copied
   * @param packagingSupport packaging support
   * @return path to the final artifact in the target folder
   * @throws IOException in case of any problems
   */
  Path packAndCopy(Path mainArtifact, PackagingSupport packagingSupport) throws IOException {

    Path mainDirectory = mainArtifact.getParent();
    if (mainDirectory == null) {
      throw new IOException("Main artifact " + mainArtifact + " needs to have a parent.");
    }
    String targetFileName = createFinalName(mainArtifact.getFileName().toString());
    Path targetFile = targetFolder.resolve(targetFileName);
    if (!Files.exists(targetFile)) {
      Files.createFile(targetFile);
    } else {
      log.warn("Target file {} exists and will be overwritten.", targetFile);
    }

    try (JarFile sourceJar = new JarFile(mainArtifact.toFile());
        ZipOutputStream targetOut = new ZipOutputStream(Files.newOutputStream(targetFile))) {
      consumeEntries(
          sourceJar,
          (entry) -> {
            if (entry.getName().endsWith(".jar")) {
              createZipEntryFromFile(
                  targetOut, mainDirectory.resolve(entry.getName()), entry.getName());
            } else {
              packagingSupport.copyAddingPrefix(entry, sourceJar, targetOut);
            }
          });
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
