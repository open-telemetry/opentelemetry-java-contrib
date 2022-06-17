/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

final class AgentFileProvider {

  static File getAgentFile() {

    verifyExistenceOfAgentJarFile();

    Path tempDirPath = createTempDir();

    Path tempAgentJarPath = createTempAgentJarFile(tempDirPath);

    deleteTempDirOnJvmExit(tempDirPath, tempAgentJarPath);

    return tempAgentJarPath.toFile();
  }

  private static void deleteTempDirOnJvmExit(Path tempDirPath, Path tempAgentJarPath) {
    if (tempDirPath != null && tempAgentJarPath != null) {
      tempAgentJarPath.toFile().deleteOnExit();
      tempDirPath.toFile().deleteOnExit();
    }
  }

  private static void verifyExistenceOfAgentJarFile() {
    CodeSource codeSource = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException("could not get agent jar location");
    }
  }

  private static Path createTempDir() {
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory("otel-agent");
    } catch (IOException e) {
      throw new IllegalStateException("Runtime attachment can't create temp directory", e);
    }
    return tempDir;
  }

  private static Path createTempAgentJarFile(Path tempDir) {
    URL url = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource().getLocation();
    try {
      return copyTo(url, tempDir, "agent.jar");
    } catch (IOException e) {
      throw new IllegalStateException(
          "Runtime attachment can't create agent jar file in temp directory", e);
    }
  }

  private static Path copyTo(URL url, Path tempDir, String fileName) throws IOException {
    Path tempFile = tempDir.resolve(fileName);
    try (InputStream in = url.openStream()) {
      Files.copy(in, tempFile);
    }
    return tempFile;
  }

  private AgentFileProvider() {}
}
