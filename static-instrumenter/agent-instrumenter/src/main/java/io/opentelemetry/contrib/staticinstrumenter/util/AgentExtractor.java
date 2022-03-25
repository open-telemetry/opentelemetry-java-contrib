/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import io.opentelemetry.contrib.staticinstrumenter.util.path.AgentPathGetter;
import io.opentelemetry.contrib.staticinstrumenter.util.path.SimplePathGetter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;

/**
 * Extracts the agent to a temporary directory. This is done to enable simple loading of agent
 * classes that need to be modified.
 */
public final class AgentExtractor {

  private final Path extractedAgent;

  public AgentExtractor(TmpDirManager tmpDirManager) throws IOException {
    this.extractedAgent = tmpDirManager.createDir("opentelemetry-javaagent");
  }

  /**
   * Extracts the agent JAR entry to a temporary directory.
   *
   * @param agentFile File object representing the OpenTelemetry javaagent file
   * @throws IOException If process of writing to file encounters problems
   */
  public Path extractAgent(File agentFile) throws IOException {
    JarFile otelJarFile = new JarFile(agentFile);

    Enumeration<JarEntry> entries = otelJarFile.entries();

    AgentPathGetter pathGetter = new SimplePathGetter();

    while (entries.hasMoreElements()) {

      JarEntry entry = entries.nextElement();

      String sanitizedName = pathGetter.getPath(entry);

      if (!entry.isDirectory()) {
        extractEntry(otelJarFile, entry, sanitizedName);
      }
    }

    return extractedAgent;
  }

  /**
   * Extracts the agent JAR entry to temporary directory under new, sanitized name.
   *
   * @param otelJarFile JarFile object representing the OpenTelemetry javaagent file
   * @param entryToSave JAR entry that is extracted
   * @param sanitized Sanitized JAR entry name
   * @throws IOException If process of writing to file encounters problems
   */
  private void extractEntry(JarFile otelJarFile, JarEntry entryToSave, String sanitized)
      throws IOException {
    int lastSlashIdx = sanitized.lastIndexOf("/");
    String path =
        sanitized.substring(0, lastSlashIdx).replace("/", System.getProperty("file.separator"));
    String className = sanitized.substring(lastSlashIdx + 1);

    Path classPackage = Files.createDirectories(extractedAgent.resolve(path));

    Files.write(
        classPackage.resolve(className),
        IOUtils.toByteArray(otelJarFile.getInputStream(entryToSave)));
  }
}
