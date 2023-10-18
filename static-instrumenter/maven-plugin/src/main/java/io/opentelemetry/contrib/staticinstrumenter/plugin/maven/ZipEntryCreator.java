/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ZipEntryCreator {

  private ZipEntryCreator() {}

  static void moveEntryUpdating(
      FileSystem targetFs, String targetPath, JarEntry sourceEntry, JarFile sourceJar)
      throws IOException {

    if (targetPath.equals(
        "META-INF/services/io.opentelemetry.javaagent.tooling.BeforeAgentListener")) {
      // TEMPORARY hack to make things pass after a BeforeAgentListener was added in the agent
      // in https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9301
      // which then causes conflict with the BeforeAgentListener in this module
      return;
    }

    Path entry = targetFs.getPath("/", targetPath);
    Files.createDirectories(entry.getParent());
    try (InputStream sourceInput = sourceJar.getInputStream(sourceEntry)) {
      Files.copy(sourceInput, entry);
    }
  }

  static void moveEntry(
      ZipOutputStream targetOut, String targetPath, JarEntry sourceEntry, JarFile sourceJar)
      throws IOException {

    ZipEntry entry = new ZipEntry(targetPath);
    try (InputStream sourceInput = sourceJar.getInputStream(sourceEntry)) {

      entry.setSize(sourceEntry.getSize());
      entry.setCompressedSize(sourceEntry.getCompressedSize());
      entry.setMethod(sourceEntry.getMethod());
      entry.setCrc(sourceEntry.getCrc());

      targetOut.putNextEntry(entry);
      sourceInput.transferTo(targetOut);
      targetOut.closeEntry();
    }
  }

  static void createZipEntryFromFile(ZipOutputStream targetOut, Path sourceFile, String entryPath)
      throws IOException {

    ZipEntry entry = new ZipEntry(entryPath);
    entry.setMethod(ZipOutputStream.DEFLATED);
    byte[] bytes = Files.readAllBytes(sourceFile);
    entry.setSize(bytes.length);
    CRC32 crc = new CRC32();
    crc.update(bytes);
    entry.setCrc(crc.getValue());

    targetOut.putNextEntry(entry);
    targetOut.write(bytes);
    targetOut.closeEntry();
  }
}
