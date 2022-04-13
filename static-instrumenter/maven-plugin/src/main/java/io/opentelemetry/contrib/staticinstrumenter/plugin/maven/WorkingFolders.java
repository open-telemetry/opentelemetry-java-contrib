/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.codehaus.plexus.util.FileUtils;

class WorkingFolders {

  @Nullable private static WorkingFolders instance;

  // folder where OTel agent is stored
  private final Path agentFolder;
  // folder where jars fro instrumentation are prepared
  private final Path preparationFolder;
  // folder where instrumentation and processing is conducted
  private final Path instrumentationFolder;
  // folder where final (instrumented, with OTel classes) JARs are stored
  private final Path finalFolder;

  private WorkingFolders(String finalFolder) throws IOException {
    agentFolder = Files.createTempDirectory("OTEL_AGENT_FOLDER");
    preparationFolder = Files.createTempDirectory("PREPARATION_FOLDER");
    instrumentationFolder = Files.createTempDirectory("INSTRUMENTATION_WORKING_FOLDER");
    this.finalFolder = Files.createDirectories(Paths.get(finalFolder));
  }

  static WorkingFolders create(String finalFolder) throws IOException {
    instance = new WorkingFolders(finalFolder);
    return instance;
  }

  static WorkingFolders getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Not created yet. Please call create() first");
    }
    return instance;
  }

  void delete() throws IOException {
    FileUtils.deleteDirectory(agentFolder.toFile());
    FileUtils.deleteDirectory(preparationFolder.toFile());
    FileUtils.deleteDirectory(instrumentationFolder.toFile());
  }

  Path agentFolder() {
    return agentFolder;
  }

  Path getPreparationFolder() {
    return preparationFolder;
  }

  Path instrumentationFolder() {
    return instrumentationFolder;
  }

  Path finalFolder() {
    return finalFolder;
  }

  void cleanWorkingFolders() throws IOException {
    FileUtils.cleanDirectory(instrumentationFolder.toFile());
    FileUtils.cleanDirectory(preparationFolder.toFile());
  }
}
