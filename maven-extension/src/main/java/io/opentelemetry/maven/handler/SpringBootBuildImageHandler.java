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
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See
 *
 * <ul>
 *   <li><a
 *       href="https://docs.spring.io/spring-boot/docs/2.6.1/maven-plugin/reference/htmlsingle/">Spring
 *       Boot Maven Plugin</a>
 *   <li><a
 *       href="https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-tools/spring-boot-maven-plugin">GitHub
 *       : spring-boot/spring-boot-project/spring-boot-tools/spring-boot-maven-plugin/</a>
 * </ul>
 */
final class SpringBootBuildImageHandler implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(SpringBootBuildImageHandler.class);

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.springframework.boot", "spring-boot-maven-plugin", "build-image"));
  }

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {

    spanBuilder.setSpanKind(SpanKind.CLIENT);

    Xpp3Dom pluginNode = executionEvent.getMojoExecution().getConfiguration();

    String imageNameAndTag = null;
    if (pluginNode != null) {
      Xpp3Dom imageNode = pluginNode.getChild("image");
      if (imageNode != null) {
        Xpp3Dom nameNode = imageNode.getChild("name");
        if (nameNode != null) {
          imageNameAndTag = nameNode.getValue();
        }
      }
    }

    String imageName;
    String imageTag;
    if (imageNameAndTag == null) {
      // default image name docker.io/library/${project.artifactId}:${project.version}
      // see
      // https://docs.spring.io/spring-boot/docs/2.6.1/maven-plugin/reference/htmlsingle/#build-image.customization
      imageName = "docker.io/library/" + executionEvent.getProject().getArtifactId();
      imageTag = executionEvent.getProject().getVersion();
    } else {
      int colonIdx = imageNameAndTag.indexOf(':');
      if (colonIdx == -1) {
        imageName = imageNameAndTag;
        imageTag = "latest";
      } else {
        imageTag = imageNameAndTag.substring(colonIdx + 1);
        imageName = imageNameAndTag.substring(0, colonIdx);
      }
    }

    // TODO handle use cases when additional additional `tags` are provided
    // cyrille didn't understand from the Spring docs how to define multiple tags in the plugin cfg
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME, imageName);
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS,
        Collections.singletonList(imageTag));

    Xpp3Dom publishNode = pluginNode == null ? null : pluginNode.getChild("publish");
    if (publishNode != null && Boolean.parseBoolean(publishNode.getValue())) {
      Xpp3Dom dockerNode = pluginNode.getChild("docker");
      Xpp3Dom registryNode = dockerNode == null ? null : dockerNode.getChild("publishRegistry");

      if (registryNode != null) {
        Xpp3Dom registryUrlNode = registryNode.getChild("url");
        String registryUrl = registryUrlNode == null ? null : registryUrlNode.getValue();

        // REGISTRY URL
        if (registryUrl != null
            && (registryUrl.startsWith("http://") || registryUrl.startsWith("https://"))) {
          spanBuilder.setAttribute(
              MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL, registryUrl);
          spanBuilder.setAttribute(UrlAttributes.URL_FULL, registryUrl);
          spanBuilder.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST");
          try {
            // Note: setting the "peer.service" helps visualization on Jaeger but
            // may not fully comply with the OTel "peer.service" spec as we don't know if the remote
            // service will be instrumented and what it "service.name" would be
            spanBuilder.setAttribute(
                MavenOtelSemanticAttributes.PEER_SERVICE, new URL(registryUrl).getHost());
          } catch (MalformedURLException e) {
            logger.debug("Ignore exception parsing container registry URL", e);
          }
        }
      }
    }
  }
}
