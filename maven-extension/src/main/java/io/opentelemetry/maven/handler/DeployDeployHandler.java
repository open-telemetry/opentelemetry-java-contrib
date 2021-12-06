/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: Later, we may prefer to have one span per actual upload than a generic span. We could
 * achieve this instrumenting the <a
 * href="https://projects.eclipse.org/projects/technology.aether">Aether library</a>
 */
public class DeployDeployHandler extends AbstractMojoGoalExecutionHandler
    implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(DeployDeployHandler.class);

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent execution) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);

    MavenProject project = execution.getProject();
    ArtifactRepository repository = project.getDistributionManagementArtifactRepository();
    if (repository == null) {
      logger.warn("OpenTelemetry: {}: deploy: no artifactRepository", project);
    } else {
      spanBuilder.setAttribute(MavenOtelSemanticAttributes.MAVEN_REPOSITORY_ID, repository.getId());
      spanBuilder.setAttribute(
          MavenOtelSemanticAttributes.MAVEN_REPOSITORY_URL, repository.getUrl());
      Authentication authentication = repository.getAuthentication();
      if (authentication != null) {
        // FIXME is there ia security question here?
        // cyrille-leclerc: no because it's just the username
        spanBuilder.setAttribute(
            MavenOtelSemanticAttributes.MAVEN_REPOSITORY_USERNAME, authentication.getUsername());
      }

      String artifactRepositoryUrl = repository.getUrl();
      if (artifactRepositoryUrl == null) {
        logger.debug("OpenTelemetry: {}: deploy: missing artifactRepository url", project);
      } else if (artifactRepositoryUrl.startsWith("https://")
          || artifactRepositoryUrl.startsWith("http://")) {
        try {
          URL repositoryUrl = new URL(artifactRepositoryUrl);
          // setting the net_peer_service helps visualization on Jaeger but
          // doesn't fully comply with the spec
          spanBuilder.setAttribute(SemanticAttributes.PEER_SERVICE, repositoryUrl.getHost());
        } catch (MalformedURLException e) {
          logger.debug("Ignore exception parsing artifact repository URL", e);
        }
        Artifact generatedArtifact = project.getArtifact();
        String artifactRootUrl = artifactRepositoryUrl;
        if (!artifactRootUrl.endsWith("/")) {
          artifactRootUrl += '/';
        }
        artifactRootUrl +=
            generatedArtifact.getGroupId().replace('.', '/')
                + '/'
                + generatedArtifact.getArtifactId()
                + '/'
                + generatedArtifact.getVersion();
        spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, artifactRootUrl);
        spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
      }
    }
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.apache.maven.plugins", "maven-deploy-plugin", "deploy"));
  }
}
