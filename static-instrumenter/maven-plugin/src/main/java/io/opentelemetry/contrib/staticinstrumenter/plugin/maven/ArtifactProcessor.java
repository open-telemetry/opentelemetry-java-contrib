/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.PackagingSupportFactory.packagingSupportFor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArtifactProcessor {

  private static final Logger log = LoggerFactory.getLogger(ArtifactProcessor.class);

  private final InstrumentationAgent agent;
  private final Instrumenter instrumenter;
  private final Unpacker unpacker;
  private final Packer packer;

  static ArtifactProcessor createProcessor(
      Path instrumentationFolder,
      Path preparationFolder,
      Path agentFolder,
      Path finalFolder,
      String finalNameSuffix)
      throws IOException {
    Unpacker unpacker = new Unpacker(preparationFolder);
    InstrumentationAgent agent = InstrumentationAgent.createFromClasspathAgent(agentFolder);
    Instrumenter instrumenter = new Instrumenter(agent, instrumentationFolder);
    Packer packer = new Packer(finalFolder, finalNameSuffix);
    return new ArtifactProcessor(unpacker, instrumenter, agent, packer);
  }

  ArtifactProcessor(
      Unpacker unpacker, Instrumenter instrumenter, InstrumentationAgent agent, Packer packer) {
    this.agent = agent;
    this.instrumenter = instrumenter;
    this.unpacker = unpacker;
    this.packer = packer;
  }

  /**
   * Instruments, repackages and enhances an artifacts. Exact steps:
   *
   * <ul>
   *   <li>unpacks artifacts into a temp dir
   *   <li>runs instrumentation agent, creating instrumented copy of a JAR and dependencies
   *   <li>packs all dependencies into a single JAR and adds open telemetry classes
   *   <li>move jar to final location, adding a suffix
   * </ul>
   */
  void process(Path artifact) throws IOException {
    PackagingSupport packagingSupport = packagingSupportFor(artifact);
    List<Path> artifactsToInstrument = unpacker.copyAndExtract(artifact, packagingSupport);
    log.info("Unpacked artifacts: {}", artifactsToInstrument);
    Path instrumentedArtifact =
        instrumenter.instrument(artifact.getFileName(), artifactsToInstrument);
    log.info("Main artifact after instrumentation: {}", instrumentedArtifact);
    agent.copyAgentClassesTo(instrumentedArtifact, packagingSupport);
    Path finalArtifact = packer.packAndCopy(instrumentedArtifact, packagingSupport);
    log.info("Final artifact: {}", finalArtifact);
  }
}
