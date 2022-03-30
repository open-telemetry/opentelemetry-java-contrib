/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import io.opentelemetry.contrib.staticinstrumenter.util.path.AgentPathGetter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a new agent JAR. */
public final class AgentCreator {

  private static final Logger logger = LoggerFactory.getLogger(AgentCreator.class);
  private final TmpDirManager dirManager;
  private final AgentPathGetter pathGetter;

  public AgentCreator(TmpDirManager dirManager, AgentPathGetter pathGetter) {
    this.dirManager = dirManager;
    this.pathGetter = pathGetter;
  }

  public void create(Path finalJarPath, Path otelJar, Map<String, byte[]> classesToInstrument)
      throws IOException {

    Path tmpPath = dirManager.getTmpFile("agent-jars", "agent-", ".jar");

    Map<String, byte[]> classesToInstrumentCopy = new HashMap<>(classesToInstrument);

    // following code mimics ByteBuddy's DynamicType.inject()
    // we don't use it directly, since it would mean copying jar every time we add one class
    try (JarInputStream in = new JarInputStream(Files.newInputStream(otelJar))) {

      Manifest manifest = in.getManifest();

      try (JarOutputStream zout =
          (manifest == null
              ? new JarOutputStream(Files.newOutputStream(tmpPath))
              : new JarOutputStream(Files.newOutputStream(tmpPath), manifest))) {
        JarEntry entry;
        // find class that we want to replace
        while ((entry = in.getNextJarEntry()) != null) {

          byte[] replacement = classesToInstrumentCopy.remove(entry.getName());

          String newName = pathGetter.getPath(entry);

          try {
            zout.putNextEntry(new JarEntry(newName));
            if (replacement == null) {
              in.transferTo(zout);
            } else {
              zout.write(replacement);
              logger.debug("Instrumented {}", entry.getName());
            }
          } catch (IOException e) {
            // ignore duplicate entries
          }

          in.closeEntry();
          zout.closeEntry();
        }

        // add extra classes
        for (Map.Entry<String, byte[]> mapEntry : classesToInstrumentCopy.entrySet()) {
          zout.putNextEntry(new JarEntry(mapEntry.getKey()));
          zout.write(mapEntry.getValue());
          zout.closeEntry();

          logger.debug("Instrumented {}", mapEntry.getKey());
        }
      }
    }
    Files.move(tmpPath, finalJarPath, REPLACE_EXISTING);
  }
}
