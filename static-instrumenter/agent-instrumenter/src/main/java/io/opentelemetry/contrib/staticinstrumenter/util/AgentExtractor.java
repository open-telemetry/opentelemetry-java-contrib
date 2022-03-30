/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import io.opentelemetry.contrib.staticinstrumenter.util.path.AgentPathGetter;
import io.opentelemetry.contrib.staticinstrumenter.util.path.SimplePathGetter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
   * Extracts the agent JAR entry to a temporary directory. It removes the inst/ prefix and
   * changes .classdata format to .class in appropriate agent classes so that they could be normally
   * loaded and modified.
   */
  public Path extractAgent(Path agentFile) throws IOException {
    JarFile otelJarFile = new JarFile(agentFile.toString());

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

  private void extractEntry(JarFile otelJavaagent, JarEntry entryToSave, String sanitizedEntryName)
      throws IOException {
    int lastSlashIdx = sanitizedEntryName.lastIndexOf("/");
    String path = sanitizedEntryName.substring(0, lastSlashIdx).replace("/", File.separator);
    String className = sanitizedEntryName.substring(lastSlashIdx + 1);

    Path classPackage = Files.createDirectories(extractedAgent.resolve(path));

    try (InputStream in = otelJavaagent.getInputStream(entryToSave);
        OutputStream out = Files.newOutputStream(classPackage.resolve(className))) {
      in.transferTo(out);
    }
  }
}
