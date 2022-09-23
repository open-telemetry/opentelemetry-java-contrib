/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.staticinstrumenter.agent.main.Main;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JarTest {

  private static final String INSTRUMENTATION_MAIN = Main.class.getCanonicalName();

  // Class contained in test-app project
  private static final String APP_MAIN =
      "io.opentelemetry.contrib.staticinstrumenter.test.HttpClientTest";

  @TempDir public Path outPath;

  @Test
  void testSampleJar() throws Exception {

    Path agentPath = Path.of(System.getProperty("agent"));
    Path noInstAgentPath = Path.of(System.getProperty("no.inst.agent"));
    // jar created in test-app module
    Path appPath = getPath("app.jar");

    // Create a jar with static instrumentation
    ProcessBuilder instrumentationProcessBuilder =
        new ProcessBuilder(
            "java",
            "-javaagent:" + agentPath,
            "-cp",
            appPath.toString(),
            INSTRUMENTATION_MAIN,
            outPath.toString());

    Process instrumentationProcess = instrumentationProcessBuilder.start();
    instrumentationProcess.waitFor(10, TimeUnit.SECONDS);

    boolean isHotJdkJvm = System.getProperty("java.vm.name").contains("OpenJDK");
    if (isHotJdkJvm) {
      InputStream errorOutput = instrumentationProcess.getErrorStream();
      String errorOutputAsString = new String(errorOutput.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(errorOutputAsString)
          .isNotEmpty()
          .contains(
              "Sharing is only supported for boot loader classes because bootstrap classpath has been appended");
    }

    assertThat(instrumentationProcess.exitValue()).isEqualTo(0);

    Path resultAppPath = outPath.resolve("app.jar");
    assertThat(resultAppPath).exists();
    assertThat(Files.exists(resultAppPath)).isTrue();

    // Run the jar with static instrumentation
    ProcessBuilder runtimeProcessBuilder =
        new ProcessBuilder(
            "java",
            "-Dotel.traces.exporter=logging",
            "-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.contextStorageProvider=default",
            "-cp",
            resultAppPath + File.pathSeparator + noInstAgentPath,
            APP_MAIN);

    Process runtimeProcess = runtimeProcessBuilder.start();
    runtimeProcess.waitFor(10, TimeUnit.SECONDS);

    assertThat(runtimeProcess.exitValue()).isEqualTo(0);

    InputStream standardOutputOfInstrumentedApp = runtimeProcess.getInputStream();
    String standardOutputAsStringIns =
        new String(standardOutputOfInstrumentedApp.readAllBytes(), StandardCharsets.UTF_8);
    assertThat(standardOutputAsStringIns).startsWith("SUCCESS - Trace parent value");
  }

  private static Path getPath(String resourceName) throws URISyntaxException {
    URL resourceURL = JarTest.class.getResource(resourceName);
    assertThat(resourceURL).isNotNull();
    return Path.of(resourceURL.toURI());
  }
}
