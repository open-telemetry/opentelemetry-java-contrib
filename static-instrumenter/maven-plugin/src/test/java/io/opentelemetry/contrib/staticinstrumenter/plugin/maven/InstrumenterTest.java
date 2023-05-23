/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJarContainsFiles;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InstrumenterTest extends AbstractTempDirTest {

  @Test
  void shouldInstrument() throws IOException {
    // given
    Instrumenter instrumenter =
        new Instrumenter(
            InstrumentationAgent.createFromClasspathAgent(tempDir.toPath()), tempDir.toPath());
    Path testJar = JarTestUtil.getResourcePath("test.jar");
    // when
    Path instrumented = instrumenter.instrument(testJar.getFileName(), singletonList(testJar));
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
