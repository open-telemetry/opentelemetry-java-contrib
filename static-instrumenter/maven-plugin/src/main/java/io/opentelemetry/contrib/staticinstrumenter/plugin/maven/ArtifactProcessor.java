/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.FrameworkSupportFactory.getFrameworkSupport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

class ArtifactProcessor {

  private final InstrumentationAgent agent;
  private final Instrumenter instrumenter;
  private final Unpacker unpacker;
  private final Packer packer;

  ArtifactProcessor(
      Unpacker unpacker, Instrumenter instrumenter, InstrumentationAgent agent, Packer packer) {
    this.agent = agent;
    this.instrumenter = instrumenter;
    this.unpacker = unpacker;
    this.packer = packer;
  }

  /**
   * Instruments, repackages and enhances an artifacts. Exact steps: - unpacks artifacts into a temp
   * dir</br> - runs instrumentation agent, creating instrumented copy of a JAR and
   * dependencies</br> - packs all dependencies into a single JAR</br> - adds open telemetry classes
   * - move jar to final location, adding a suffix</br>
   */
  Path process(Path artifact) throws IOException {
    FrameworkSupport frameworkSupport = getFrameworkSupport(artifact);
    List<Path> artifactsToInstrument = unpacker.copyAndUnpack(artifact, frameworkSupport);
    Path instrumentedArtifact =
        instrumenter.instrument(artifact.getFileName(), artifactsToInstrument);
    agent.copyAgentClassesTo(instrumentedArtifact, frameworkSupport);
    return packer.packAndCopy(instrumentedArtifact, frameworkSupport);
  }
}
