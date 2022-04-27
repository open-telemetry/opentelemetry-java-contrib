/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JarTest {

  private static final String INSTRUMENTATION_MAIN =
      "io.opentelemetry.contrib.staticinstrumenter.agent.main.Main";
  private static final String APP_MAIN = "org.example.SingleGetter";

  @TempDir public Path outPath;

  @Test
  void testSampleJar() throws IOException, InterruptedException {
    Path agentPath = Path.of(System.getProperty("agent"));
    Path noInstAgentPath = Path.of(System.getProperty("no.inst.agent"));
    // jar created in test-app module
    Path appPath = getPath("app.jar");

    ProcessBuilder instrumentationProcessBuilder =
        new ProcessBuilder(
            "java",
            "-javaagent:" + agentPath,
            "-cp",
            appPath.toString(),
            INSTRUMENTATION_MAIN,
            outPath.toString());

    Process instrumentationProcess = instrumentationProcessBuilder.start();
    InputStream instrumentationIn = instrumentationProcess.getErrorStream();
    instrumentationProcess.waitFor(10, TimeUnit.SECONDS);

    String instrumentationLog =
        new String(instrumentationIn.readAllBytes(), StandardCharsets.UTF_8);
    assertThat(instrumentationProcess.exitValue()).isEqualTo(0);
    assertThat(instrumentationLog).isNotEmpty();

    Path resultAppPath = outPath.resolve("app.jar");
    assertThat(Files.exists(resultAppPath)).isTrue();

    ProcessBuilder runtimeProcessBuilder =
        new ProcessBuilder(
            "java",
            "-Dotel.traces.exporter=logging",
            "-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default",
            "-cp",
            String.format("%s:%s", resultAppPath, noInstAgentPath),
            APP_MAIN);

    Process runtimeProcess = runtimeProcessBuilder.start();
    InputStream err = runtimeProcess.getErrorStream();
    runtimeProcess.waitFor(10, TimeUnit.SECONDS);

    assertThat(runtimeProcess.exitValue()).isEqualTo(0);
    assertThat(new String(err.readAllBytes(), StandardCharsets.UTF_8))
        .startsWith(
            "[main] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'HTTP GET'");
  }

  private static Path getPath(String resourceName) {
    URL resourceURL = JarTest.class.getResource(resourceName);
    assertThat(resourceURL).isNotNull();
    return Path.of(resourceURL.getPath());
  }
}
