/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenTelemetryInstrumenterMojoTest {

  @Test
  void shouldInstrumentSampleApplication(@TempDir File tempdir) throws Exception {
    // given
    OpenTelemetryInstrumenterMojo mojo = new OpenTelemetryInstrumenterMojo();
    Path testApp = Paths.get(JarTestUtil.getResourcePath("test-http-app.jar"));
    // when
    mojo.executeInternal(tempdir.getPath(), "-instrumented", Collections.singletonList(testApp));
    // then
    Path instrumentedApp = tempdir.toPath().resolve("test-http-app-instrumented.jar");
    assertThat(Files.exists(instrumentedApp)).isTrue();
    verifyApplicationByExampleRun(instrumentedApp);
  }

  /**
   * Test application does an http call using Apache HTTP client. If a response contains
   * "Traceparent" header (result of autoinstrumentation), application writes "SUCCESS" to system
   * out.
   */
  private static void verifyApplicationByExampleRun(Path instrumentedApp) throws Exception {
    ProcessBuilder pb =
        new ProcessBuilder(
                "java",
                "-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default",
                "-jar",
                instrumentedApp.toString())
            .redirectErrorStream(true);
    Process process = pb.start();
    process.waitFor();
    String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
    assertThat(output).contains("SUCCESS");
  }
}
