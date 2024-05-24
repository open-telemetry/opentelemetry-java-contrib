/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.net.MalformedURLException;
import java.net.URL;
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
      try {
        // Note: setting the "peer.service" helps visualization on Jaeger but
        // may not fully comply with the OTel "peer.service" spec as we don't know if the remote
        // service will be instrumented and what it "service.name" would be
        spanBuilder.setAttribute(
            MavenOtelSemanticAttributes.PEER_SERVICE, new URL(artifactRepositoryUrl).getHost());
      } catch (MalformedURLException e) {
        logger.debug("Ignore exception parsing artifact repository URL", e);
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
      spanBuilder.setAttribute(UrlAttributes.URL_FULL, artifactRootUrl);
      spanBuilder.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST");
    }
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.apache.maven.plugins", "maven-deploy-plugin", "deploy"));
  }
}
