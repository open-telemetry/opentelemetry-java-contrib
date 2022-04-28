/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instrumenter {

  private static final Logger log = LoggerFactory.getLogger(Instrumenter.class);

  private final InstrumentationAgent agent;
  private final Path targetFolder;

  public Instrumenter(InstrumentationAgent agent, Path targetFolder) {
    this.agent = agent;
    this.targetFolder = targetFolder;
  }

  public Path instrument(Path mainArtifact, List<Path> artifactsToInstrument) throws IOException {

    runInstrumentationProcess(artifactsToInstrument);
    return targetFolder.resolve(mainArtifact);
  }

  private void runInstrumentationProcess(List<Path> artifactsToInstrument) throws IOException {
    ProcessBuilder processBuilder =
        agent
            .getInstrumentationProcess(toClasspath(artifactsToInstrument), targetFolder)
            .redirectErrorStream(true);
    log.debug("Instrumentation process: {}", processBuilder.command());
    Process process = processBuilder.start();

    try {
      int ret = process.waitFor();
      if (ret != 0) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        process.getInputStream().transferTo(output);
        StringBuilder builder =
            new StringBuilder(
                    "The instrumentation process for JAR dependencies finished with exit value: ")
                .append(ret)
                .append("\nSystem out:\n")
                .append(output.toString(Charset.defaultCharset()));
        throw new IOException(builder.toString());
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IOException(ie);
    }
  }

  private static String toClasspath(List<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }
}
