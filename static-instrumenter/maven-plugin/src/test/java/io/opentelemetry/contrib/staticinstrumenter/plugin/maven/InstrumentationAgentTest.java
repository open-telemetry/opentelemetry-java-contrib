/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstrumentationAgentTest {

  @Test
  void shouldCreateAgentFromClasspath(@TempDir File targetFolder) throws Exception {
    // given
    // when
    InstrumentationAgent.createFromClasspathAgent(targetFolder.toPath());
    // then
    assertThat(Files.exists(targetFolder.toPath().resolve(InstrumentationAgent.JAR_FILE_NAME)))
        .isTrue();
  }

  @Test
  void shouldCopyAgentClasses(@TempDir File targetFolder) throws IOException {
    // given
    Path targetFile = targetFolder.toPath().resolve("copied-agent.jar");
    Files.createFile(targetFile);
    InstrumentationAgent agent =
        InstrumentationAgent.createFromClasspathAgent(targetFolder.toPath());
    // when
    agent.copyAgentClassesTo(targetFile, PackagingSupport.EMPTY);
    // then - verify jar content
    JarSupport.consumeEntries(
        new JarFile(targetFile.toFile()),
        (entry) -> {
          assertThat(entry.getName()).doesNotEndWith(".classdata");
          assertThat(entry.getName()).doesNotStartWith("inst/");
        });
  }
}
