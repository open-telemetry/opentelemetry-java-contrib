/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarSupport.consumeEntries;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.moveEntryUpdating;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationAgent {

  // TODO: change after the new static-instr capable agent is released
  public static final String JAR_FILE_NAME = "opentelemetry-javaagent.jar";
  // TODO: change after the new static-instr capable agent is released
  public static final String MAIN_CLASS = "io.opentelemetry.javaagent.StaticInstrumenter";

  private static final Logger logger = LoggerFactory.getLogger(InstrumentationAgent.class);

  private final String agentPath;
  private final JarFile agentJar;

  private InstrumentationAgent(String agentPath) throws IOException {
    this.agentPath = agentPath;
    this.agentJar = new JarFile(agentPath);
  }

  /**
   * Creates new instance using {@value #JAR_FILE_NAME} classpath agent. Agent is copied to the
   * target folder.
   */
  public static InstrumentationAgent createFromClasspathAgent(Path targetFolder)
      throws IOException {

    Path targetPath = targetFolder.resolve(JAR_FILE_NAME);
    InputStream agentSource =
        InstrumentationAgent.class.getClassLoader().getResourceAsStream(JAR_FILE_NAME);
    if (agentSource == null) {
      throw new IllegalStateException(
          "Instrumented OTel agent not found in class path and JAR name: " + JAR_FILE_NAME);
    }
    try {
      Files.copy(agentSource, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      logger.error("Couldn't copy agent JAR using class resource: {}.", JAR_FILE_NAME);
      throw exception;
    }
    return new InstrumentationAgent(targetPath.toString());
  }

  /**
   * Returns the ProcessBuilder for instrumentation process.
   *
   * @param classpath classpath for instrumentation process (list of JARs to instrument, separated
   *     with path separator)
   * @param outputFolder output folder
   * @return ProcessBuilder for instrumentation process
   */
  public ProcessBuilder getInstrumentationProcess(String classpath, Path outputFolder) {
    return new ProcessBuilder(
        "java",
        "-Dotel.instrumentation.internal-class-loader.enabled=false",
        String.format("-javaagent:%s", agentPath),
        "-cp",
        classpath,
        MAIN_CLASS,
        outputFolder.toString());
  }

  /**
   * Adds classes from OpenTelemetry javaagent JAR to target file. Removes the shading and replaces
   * classdata file extension with class file extension.
   */
  public void copyAgentClassesTo(Path targetFile, PackagingSupport packagingSupport)
      throws IOException {

    Set<String> existing = allNames(targetFile);
    try (FileSystem targetFs = FileSystems.newFileSystem(targetFile, null)) {
      consumeEntries(
          agentJar,
          (entry) -> {
            String entryName = entry.getName();
            if (isInstrumentationClass(entry)) {
              String modifiedName = modifyAgentEntryName(entryName, packagingSupport);
              if (!existing.contains(modifiedName)) {
                moveEntryUpdating(targetFs, modifiedName, entry, agentJar);
              }
            }
          });
    }
  }

  private static Set<String> allNames(Path jarPath) throws IOException {
    Set<String> result = new HashSet<>();
    try (JarFile jar = new JarFile(jarPath.toFile())) {
      consumeEntries(jar, entry -> result.add(entry.getName()));
    }
    return result;
  }

  private static boolean isInstrumentationClass(JarEntry entry) {
    return (entry.getName().startsWith("inst/") || entry.getName().startsWith("io/"))
        && !entry.isDirectory();
  }

  private static String modifyAgentEntryName(String entryName, PackagingSupport packagingSupport) {
    String prefix = packagingSupport.getClassesPrefix();
    String newEntryPath = entryName.replace(".classdata", ".class");
    return newEntryPath.replace("inst/", prefix);
  }
}
