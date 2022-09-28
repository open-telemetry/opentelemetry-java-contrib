/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJarContainsFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class InstrumenterTest extends AbstractTempDirTest {

  @Test
  void shouldInstrument() throws IOException {
    // given
    Instrumenter instrumenter =
        new Instrumenter(
            InstrumentationAgent.createFromClasspathAgent(tempDir.toPath()), tempDir.toPath());
    Path testJar = Paths.get(JarTestUtil.getResourcePath("test.jar"));
    // when
    Path instrumented =
        instrumenter.instrument(testJar.getFileName(), Collections.singletonList(testJar));
    // then
    // got the target file right?
    assertJarContainsFiles(
        instrumented,
        "META-INF/MANIFEST.MF",
        "test/NotInstrumented.class",
        "test/TestClass.class",
        "lib/firstNested.jar",
        "lib/secondNested.jar");
  }
}
