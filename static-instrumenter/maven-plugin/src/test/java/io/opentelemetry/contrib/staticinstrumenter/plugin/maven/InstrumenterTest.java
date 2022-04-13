package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstrumenterTest {

  @Test
  void shouldInstrument(@TempDir File tempDir) throws IOException {
    // given
    Instrumenter instrumenter = new Instrumenter(
        InstrumentationAgent.createFromClasspathAgent(tempDir.toPath()), tempDir.toPath());
    Path testJar = Paths.get(JarTestUtil.getResourcePath("test.jar"));
    // when
    Path instrumented = instrumenter.instrument(testJar.getFileName(), Arrays.asList(testJar));
    // then
    // got the target file right?
    assertJar(
        instrumented,
        "META-INF/MANIFEST.MF",
        "test/NotInstrumented.class",
        "test/TestClass.class",
        "lib/firstNested.jar",
        "lib/secondNested.jar");
  }
}