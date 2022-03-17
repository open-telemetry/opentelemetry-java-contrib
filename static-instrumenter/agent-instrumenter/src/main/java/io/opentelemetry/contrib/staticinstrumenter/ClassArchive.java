/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents an archive storing classes (JAR, WAR). */
class ClassArchive {

  interface Factory {
    default ClassArchive createFor(JarFile source, Map<String, byte[]> instrumentedClasses) {
      return new ClassArchive(source, instrumentedClasses);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ClassArchive.class);

  private final JarFile source;
  private final Map<String, byte[]> instrumentedClasses;

  ClassArchive(JarFile source, Map<String, byte[]> instrumentedClasses) {
    this.source = source;
    this.instrumentedClasses = instrumentedClasses;
  }

  void copyAllClassesTo(ZipOutputStream outJar) throws IOException {

    Enumeration<JarEntry> inEntries = source.entries();
    while (inEntries.hasMoreElements()) {
      copyEntry(inEntries.nextElement(), outJar);
    }
  }

  private void copyEntry(JarEntry inEntry, ZipOutputStream outJar) throws IOException {
    String inEntryName = inEntry.getName();
    ZipEntry outEntry =
        inEntryName.endsWith(".jar") ? new ZipEntry(inEntry) : new ZipEntry(inEntryName);

    try (InputStream entryInputStream = getInputStreamForEntry(inEntry, outEntry)) {
      try {
        outJar.putNextEntry(outEntry);
        entryInputStream.transferTo(outJar);
        outJar.closeEntry();
      } catch (ZipException e) {
        if (!isEntryDuplicate(e)) {
          logger.error("Error while creating entry: {}", outEntry.getName(), e);
          throw e;
        }
      }
    }
  }

  private static boolean isEntryDuplicate(ZipException ze) {
    return ze.getMessage() != null && ze.getMessage().contains("duplicate");
  }

  private InputStream getInputStreamForEntry(JarEntry inEntry, ZipEntry outEntry)
      throws IOException {

    InputStream entryIn = null;
    ArchiveEntry entry = ArchiveEntry.fromZipEntryName(inEntry.getName());
    if (entry.shouldInstrument()) {
      String className = entry.getName();
      byte[] modified = instrumentedClasses.get(entry.getPath());
      if (modified != null) {
        logger.debug("Found instrumented class: " + className);
        entryIn = new ByteArrayInputStream(modified);
        outEntry.setSize(modified.length);
      }
    }
    return entryIn == null ? source.getInputStream(inEntry) : entryIn;
  }
}
