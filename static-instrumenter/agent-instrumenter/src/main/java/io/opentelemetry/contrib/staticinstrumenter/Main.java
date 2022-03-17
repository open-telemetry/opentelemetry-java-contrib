/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final Main INSTANCE = new Main(new ClassArchive.Factory() {});

  private final ClassArchive.Factory classArchiveFactory;

  // key is slashy name, not dotty
  private final Map<String, byte[]> instrumentedClasses = new ConcurrentHashMap<>();
  private final Map<String, byte[]> additionalClasses = new ConcurrentHashMap<>();

  @SuppressWarnings("ThreadLocalUsage")
  private final ThreadLocal<TransformedClass> currentClass = new ThreadLocal<>();

  public static void main(String[] args) throws Exception {

    if (args.length != 1) {
      printUsage();
      return;
    }

    File outDir = new File(args[0]);
    if (!outDir.exists()) {
      outDir.mkdir();
    }

    String classPath = System.getProperty("java.class.path");
    logger.debug("Classpath (jars list): " + classPath);
    String[] jarsList = classPath.split(File.pathSeparator);

    getInstance().saveTransformedJarsTo(jarsList, outDir);
  }

  @SuppressWarnings("SystemOut")
  private static void printUsage() {
    System.out.println(
        "OpenTelemetry Java Static Instrumenter\n"
            + "Usage:\njava "
            + Main.class.getCanonicalName()
            + " <output directory> (where instrumented archives will be stored)");
  }

  public static Main getInstance() {
    return INSTANCE;
  }

  public static ClassFileTransformer getPreTransformer() {
    return new PreTransformer();
  }

  public static ClassFileTransformer getPostTransformer() {
    return new PostTransformer();
  }

  // for testing purposes
  Main(ClassArchive.Factory classArchiveFactory) {
    this.classArchiveFactory = classArchiveFactory;
  }

  // FIXME: java 9 / jmod support, proper handling of directories
  // FIXME: jmod in particular introduces weirdness with adding helpers to the dependencies

  /**
   * Copies all class archives (JARs, WARs) to outDir. Classes that were instrumented and stored in
   * instrumentedClasses will get replaced with the new version. All classes added to
   * additionalClasses will be added to the new archive.
   *
   * @param outDir directory where jars will be written
   * @throws IOException in case of file operation problem
   */
  public void saveTransformedJarsTo(String[] jarsList, File outDir) throws IOException {

    for (String pathItem : jarsList) {
      logger.info("Classpath item processed: " + pathItem);
      if (isArchive(pathItem)) {
        saveArchiveTo(new File(pathItem), outDir);
      }
    }
  }

  private static boolean isArchive(String pathItem) {
    return (pathItem.endsWith(".jar") || pathItem.endsWith(".war"));
  }

  // FIXME: don't "instrument" our agent jar
  // FIXME: detect and warn on signed jars (and drop the signing bits)
  // FIXME: multiple jars with same name
  private void saveArchiveTo(File inFile, File outDir) throws IOException {

    try (JarFile inJar = new JarFile(inFile);
        ZipOutputStream outJar = outJarFor(outDir, inFile)) {
      ClassArchive inClassArchive = classArchiveFactory.createFor(inJar, instrumentedClasses);
      inClassArchive.copyAllClassesTo(outJar);
      injectAdditionalClassesTo(outJar);
    }
  }

  private static ZipOutputStream outJarFor(File outDir, File inFile) throws FileNotFoundException {
    File outFile = new File(outDir, inFile.getName());
    return new ZipOutputStream(new FileOutputStream(outFile));
  }

  // FIXME: only relevant additional classes should be injected
  private void injectAdditionalClassesTo(ZipOutputStream outJar) throws IOException {
    for (Map.Entry<String, byte[]> entry : additionalClasses.entrySet()) {
      String className = entry.getKey();
      byte[] classData = entry.getValue();

      ZipEntry newEntry = new ZipEntry(className);
      outJar.putNextEntry(newEntry);
      if (classData != null) {
        newEntry.setSize(classData.length);
        outJar.write(classData);
      }
      outJar.closeEntry();

      logger.debug("Additional class added: {}", className);
    }
  }

  public TransformedClass getCurrentClass() {
    return currentClass.get();
  }

  public void setCurrentClass(TransformedClass clazz) {
    currentClass.set(clazz);
  }

  public Map<String, byte[]> getInstrumentedClasses() {
    return instrumentedClasses;
  }

  public Map<String, byte[]> getAdditionalClasses() {
    return additionalClasses;
  }
}
