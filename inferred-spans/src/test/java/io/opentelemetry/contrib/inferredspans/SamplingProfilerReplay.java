/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Can be used in combination with the files created by {@link
 * ProfilingConfiguration#backupDiagnosticFiles} to replay the creation of profiler-inferred spans.
 * This is useful, for example, to troubleshoot why {@link
 * co.elastic.apm.agent.impl.transaction.Span#childIds} are set as expected.
 */
public class SamplingProfilerReplay {

  private static final Logger logger = Logger.getLogger(SamplingProfilerReplay.class.getName());

  public static void main(String[] args) throws Exception {
    ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    File activationEventsFile = File.createTempFile("activations", ".dat");
    activationEventsFile.deleteOnExit();
    File jfrFile = File.createTempFile("traces", ".jfr");
    jfrFile.deleteOnExit();

    try (ProfilerTestSetup setup =
        ProfilerTestSetup.create(
            config ->
                config
                    .startScheduledProfiling(false)
                    .activationEventsFile(activationEventsFile)
                    .jfrFile(jfrFile))) {
      Path baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "profiler");
      List<Path> activationFiles =
          Files.list(baseDir)
              .filter(p -> p.toString().endsWith("activations.dat"))
              .sorted()
              .collect(Collectors.toList());
      List<Path> traceFiles =
          Files.list(baseDir)
              .filter(p -> p.toString().endsWith("traces.jfr"))
              .sorted()
              .collect(Collectors.toList());
      if (traceFiles.size() != activationFiles.size()) {
        throw new IllegalStateException();
      }
      for (int i = 0; i < activationFiles.size(); i++) {
        logger.log(
            Level.INFO,
            "processing {0} {1}",
            new Object[] {activationFiles.get(i), traceFiles.get(i)});
        setup.profiler.copyFromFiles(activationFiles.get(i), traceFiles.get(i));
        setup.profiler.processTraces();
      }
      logger.log(Level.INFO, "{0}", setup.getSpans());
    }
  }
}
