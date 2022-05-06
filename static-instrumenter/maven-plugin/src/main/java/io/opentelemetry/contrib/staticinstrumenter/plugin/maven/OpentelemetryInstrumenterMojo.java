/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(
    name = "instrument",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class OpenTelemetryInstrumenterMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  @Nullable
  private MavenProject project;

  @Parameter(readonly = true)
  @Nullable
  private String artifactName;

  @Parameter(readonly = true)
  @Nullable
  private String outputFolder;

  @Parameter(readonly = true, defaultValue = "-instrumented")
  @Nullable
  private String suffix;

  private final Logger logger = LoggerFactory.getLogger(OpentelemetryInstrumenterMojo.class);

  /**
   * Conducts the process of application target file instrumentation. Cleans the temporary
   * directories. Sets the output folder for instrumented target file. Executes the instrumentation
   * process of artifacts chosen by the user.
   */
  @Override
  public void execute() throws MojoExecutionException {

    try {
      if (project == null) {
        throw new MojoExecutionException("Project not set");
      }

      String finalFolder =
          (outputFolder == null ? project.getBuild().getDirectory() : outputFolder);
      String finalNameSuffix = (suffix == null ? "" : suffix);

      List<Path> artifactsToInstrument =
          new ProjectModel(project).chooseForInstrumentation(artifactName);
      executeInternal(finalFolder, finalNameSuffix, artifactsToInstrument);

    } catch (Exception ioe) {
      throw new MojoExecutionException("Exception executing plugin", ioe);
    }
  }

  @VisibleForTesting
  final void executeInternal(
      String finalFolder, String finalNameSuffix, List<Path> artifactsToInstrument)
      throws IOException {

    WorkingFolders workingFolders = null;
    try {
      workingFolders = new WorkingFolders(finalFolder);
      ArtifactProcessor artifactProcessor = createProcessor(workingFolders, finalNameSuffix);
      for (Path artifact : artifactsToInstrument) {
        logger.info("Processing artifact: {}", artifact);
        artifactProcessor.process(artifact);
        workingFolders.cleanWorkingFolders();
      }
    } finally {
      try {
        if (workingFolders != null) {
          workingFolders.delete();
        }
      } catch (Exception e) {
        // ignored
      }
    }
  }

  private static ArtifactProcessor createProcessor(
      WorkingFolders workingFolders, String finalNameSuffix) throws IOException {
    return ArtifactProcessor.createProcessor(
        workingFolders.instrumentationFolder(),
        workingFolders.getPreparationFolder(),
        workingFolders.agentFolder(),
        workingFolders.finalFolder(),
        finalNameSuffix);
  }
}
