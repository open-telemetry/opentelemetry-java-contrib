/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class AgentFileProvider {

  private final String agentJarResourceName;

  AgentFileProvider(String agentJarResourceName) {
    this.agentJarResourceName = agentJarResourceName;
  }

  File getAgentFile() {

    Path tempDirPath = createTempDir();

    Path tempAgentJarPath = createTempAgentJarFileIn(tempDirPath);

    deleteTempDirOnJvmExit(tempDirPath, tempAgentJarPath);

    return tempAgentJarPath.toFile();
  }

  private static void deleteTempDirOnJvmExit(Path tempDirPath, Path tempAgentJarPath) {
    tempAgentJarPath.toFile().deleteOnExit();
    tempDirPath.toFile().deleteOnExit();
  }

  private static Path createTempDir() {
    Path tempDir;
    try {
      tempDir = Files.createTempDirectory("otel-agent");
    } catch (IOException e) {
      throw new RuntimeAttachException(
          "Runtime attachment can't create a temp directory. Are you using a read-only file system?",
          e);
    }
    return tempDir;
  }

  private Path createTempAgentJarFileIn(Path tempDir) {
    Path agentJarPath = tempDir.resolve("agent.jar");
    try (InputStream jarAsInputStream =
        AgentFileProvider.class.getResourceAsStream(this.agentJarResourceName)) {
      if (jarAsInputStream == null) {
        throw new RuntimeAttachException(this.agentJarResourceName + " resource can't be found");
      }
      Files.copy(jarAsInputStream, agentJarPath);
    } catch (IOException e) {
      throw new RuntimeAttachException(
          "Runtime attachment can't create an agent jar file in temp directory", e);
    }
    return agentJarPath;
  }
}
