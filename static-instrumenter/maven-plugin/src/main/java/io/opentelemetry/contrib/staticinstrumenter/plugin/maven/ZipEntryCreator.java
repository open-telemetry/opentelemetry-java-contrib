/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ZipEntryCreator {

  private ZipEntryCreator() {}

  static void moveEntry(
      ZipOutputStream targetOut, String targetPath, JarEntry sourceEntry, JarFile sourceJar)
      throws IOException {

    ZipEntry entry = new ZipEntry(targetPath);
    try (InputStream sourceInput = sourceJar.getInputStream(sourceEntry)) {

      entry.setSize(sourceEntry.getSize());
      entry.setCompressedSize(sourceEntry.getCompressedSize());
      entry.setCrc(sourceEntry.getCrc());

      targetOut.putNextEntry(entry);
      sourceInput.transferTo(targetOut);
      targetOut.closeEntry();
    }
  }

  static void createZipEntryFromFile(ZipOutputStream targetOut, Path sourceFile, String entryPath)
      throws IOException {

    ZipEntry entry = new ZipEntry(entryPath);
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
