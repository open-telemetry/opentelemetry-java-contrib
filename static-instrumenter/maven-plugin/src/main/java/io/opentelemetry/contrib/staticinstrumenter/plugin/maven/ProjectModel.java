/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains methods to retrieve all artifacts created in Maven project. */
class ProjectModel {

  private static final Logger logger = LoggerFactory.getLogger(ProjectModel.class);

  private final MavenProject project;

  ProjectModel(MavenProject project) {
    this.project = project;
  }

  /**
   * Returns a list of all Maven project artifacts (attached included) or artifact specified by
   * user.
   */
  List<Path> chooseForInstrumentation(@Nullable String artifactName) {

    List<Path> allArtifacts = getAllAsFiles();

    if (artifactName == null) {
      if (logger.isDebugEnabled()) {
        String names =
            Arrays.toString(allArtifacts.stream().map(Path::toFile).map(File::getName).toArray());
        logger.debug("Artifact name not provided. Defaults to instrument all artifacts: {}", names);
      }
      return allArtifacts;
    }

    for (Path artifactFile : allArtifacts) {
      if (artifactName.equals(artifactFile.toFile().getName())) {
        return new ArrayList<>(Collections.singletonList(artifactFile));
      }
    }
    String message = "Artifact with name " + artifactName + " not found.";
    logger.error(message);
    throw new IllegalArgumentException(message);
  }

  private List<Path> getAllAsFiles() {
    List<Path> artifactFiles = new ArrayList<>();
    artifactFiles.add(project.getArtifact().getFile().toPath());
    project.getAttachedArtifacts().stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .forEach(artifactFiles::add);
    return artifactFiles;
  }
}
