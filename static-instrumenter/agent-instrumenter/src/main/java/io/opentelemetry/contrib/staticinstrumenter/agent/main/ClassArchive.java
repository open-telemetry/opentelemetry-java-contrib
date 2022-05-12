/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.agent.main;

import io.opentelemetry.contrib.staticinstrumenter.util.SystemLogger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/** Represents an archive storing classes (JAR, WAR). */
class ClassArchive {

  interface Factory {
    ClassArchive createFor(JarFile source, Map<String, byte[]> instrumentedClasses);
  }

  private static final SystemLogger logger = SystemLogger.getLogger(ClassArchive.class);

  private final JarFile source;
  private final Map<String, byte[]> instrumentedClasses;

  ClassArchive(JarFile source, Map<String, byte[]> instrumentedClasses) {
    this.source = source;
    this.instrumentedClasses = instrumentedClasses;
  }

  void copyAllClassesTo(JarOutputStream outJar) throws IOException {

    Enumeration<JarEntry> inEntries = source.entries();
    while (inEntries.hasMoreElements()) {
      copyEntry(inEntries.nextElement(), outJar);
    }
  }

  private void copyEntry(JarEntry inEntry, JarOutputStream outJar) throws IOException {
    String inEntryName = inEntry.getName();
    ZipEntry outEntry =
        inEntryName.endsWith(".jar") ? new ZipEntry(inEntry) : new ZipEntry(inEntryName);

    try (InputStream entryInputStream = getInputStreamForEntry(inEntry, outEntry)) {
      outJar.putNextEntry(outEntry);
      entryInputStream.transferTo(outJar);
      outJar.closeEntry();
    } catch (ZipException e) {
      if (!isEntryDuplicate(e)) {
        logger.error("Error while creating entry: ", e, outEntry.getName());
        throw e;
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
      try {
        ClassLoader loader = new URLClassLoader(new URL[0]);
        loader.loadClass(className);

        byte[] modified = instrumentedClasses.get(entry.getPath());
        if (modified != null) {
          logger.debug("Found instrumented class: {}", className);
          entryIn = new ByteArrayInputStream(modified);
          outEntry.setSize(modified.length);
        }
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        logger.error("Problem with class: {}", e, className);
      }
    }
    return entryIn == null ? source.getInputStream(inEntry) : entryIn;
  }
}
