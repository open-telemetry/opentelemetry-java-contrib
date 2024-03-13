/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.maven.semconv.SemconvStability;
import io.opentelemetry.semconv.SemanticAttributes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: Later, we may prefer to have one span per actual upload than a generic span. We could
 * achieve this instrumenting the <a
 * href="https://projects.eclipse.org/projects/technology.aether">Aether library</a>
 */
final class MavenDeployHandler implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(MavenDeployHandler.class);

  @SuppressWarnings("deprecation") // until old http semconv are dropped
  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent execution) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);

    MavenProject project = execution.getProject();
    ArtifactRepository optRepository = project.getDistributionManagementArtifactRepository();

    if (optRepository == null) {
      return;
    }
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_REPOSITORY_ID, optRepository.getId());
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_REPOSITORY_URL, optRepository.getUrl());

    String artifactRepositoryUrl = optRepository.getUrl();
    if (artifactRepositoryUrl != null
        && (artifactRepositoryUrl.startsWith("https://")
            || artifactRepositoryUrl.startsWith("http://"))) {
      String artifactoryRepositoryHost;
      try {
        URI artifactoryRepositoryUri = new URI(artifactRepositoryUrl);
        artifactoryRepositoryHost = artifactoryRepositoryUri.getHost();
      } catch (URISyntaxException e) {
        logger.debug("Ignore exception parsing artifact repository URL", e);
        artifactoryRepositoryHost = "unknown";
      }

      Artifact artifact = project.getArtifact();
      String artifactRootUrl = artifactRepositoryUrl;
      if (!artifactRootUrl.endsWith("/")) {
        artifactRootUrl += '/';
      }
      artifactRootUrl +=
          artifact.getGroupId().replace('.', '/')
              + '/'
              + artifact.getArtifactId()
              + '/'
              + artifact.getVersion();

      if (SemconvStability.emitStableHttpSemconv()) {
        spanBuilder
            .setAttribute(SemanticAttributes.URL_FULL, artifactRootUrl)
            .setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, "POST")
            .setAttribute(SemanticAttributes.SERVER_ADDRESS, artifactoryRepositoryHost);
      }

      if (SemconvStability.emitOldHttpSemconv()) {
        spanBuilder
            .setAttribute(SemanticAttributes.HTTP_URL, artifactRootUrl)
            .setAttribute(SemanticAttributes.HTTP_METHOD, "POST")
            .setAttribute(SemanticAttributes.NET_PEER_NAME, artifactoryRepositoryHost);
      }
    }
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.apache.maven.plugins", "maven-deploy-plugin", "deploy"));
  }
}
