/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// Workaround for https://github.com/junit-team/junit5/issues/2811
abstract class AbstractTempDirTest {

  File tempDir;

  @BeforeEach
  public void before() throws IOException {
    tempDir = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
  }

  @AfterEach
  void after() {
    tempDir.deleteOnExit();
  }
}
