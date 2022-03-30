/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.internals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents an archive storing classes (JAR, WAR). */
public class ClassArchive {

  public interface Factory {
    ClassArchive createFor(JarFile source, Map<String, byte[]> instrumentedClasses);
  }

  private static final Logger logger = LoggerFactory.getLogger(ClassArchive.class);

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
        logger.error("Error while creating entry: {}", outEntry.getName(), e);
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
        // This line triggers the instrumentation of by forcefully loading the class.
        // We have to "feed" the class to the agent ourselves,
        // since during static instrumentation process the classes in user's app
        // have no guarantee of getting loaded otherwise.
        // TODO: load classes on archive's separate classloader
        Class.forName(className, false, ClassLoader.getSystemClassLoader());
        byte[] modified = instrumentedClasses.get(entry.getPath());
        if (modified != null) {
          logger.debug("Found instrumented class: {}", className);
          entryIn = new ByteArrayInputStream(modified);
          outEntry.setSize(modified.length);
        }
      } catch (ClassNotFoundException | NoClassDefFoundError cdp) {
        logger.warn(
            "Could not load class: "
                + className
                + ". Make sure the dependencies are correctly set.",
            cdp);
      }
    }
    return entryIn == null ? source.getInputStream(inEntry) : entryIn;
  }
}
