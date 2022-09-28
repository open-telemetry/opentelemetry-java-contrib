/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class InstrumentationAgentTest extends AbstractTempDirTest {

  @Test
  void shouldCreateAgentFromClasspath() throws Exception {
    // given
    // when
    InstrumentationAgent.createFromClasspathAgent(tempDir.toPath());
    // then
    assertThat(Files.exists(tempDir.toPath().resolve(InstrumentationAgent.JAR_FILE_NAME))).isTrue();
  }

  @Test
  void shouldCopyAgentClasses() throws IOException {
    // given
    Path targetFile = tempDir.toPath().resolve("copied-agent.jar");
    Path sourceJar = getResourcePath("test.jar");
    Files.copy(sourceJar, targetFile);
    InstrumentationAgent agent = InstrumentationAgent.createFromClasspathAgent(tempDir.toPath());
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
