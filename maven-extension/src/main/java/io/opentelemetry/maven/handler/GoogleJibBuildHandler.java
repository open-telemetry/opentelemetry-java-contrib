/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** See https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin */
public class GoogleJibBuildHandler implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GoogleJibBuildHandler.class);

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("com.google.cloud.tools", "jib-maven-plugin", "build"));
  }

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);

    Optional<Xpp3Dom> pluginNode =
        Optional.ofNullable(executionEvent.getMojoExecution().getConfiguration());
    Optional<Xpp3Dom> toNode = pluginNode.map(plugin -> plugin.getChild("to"));
    Optional<Xpp3Dom> imageNode = toNode.map(to -> to.getChild("image"));
    String imageName;
    List<String> imageTags = new ArrayList<>();
    Optional<String> imageNameAndTagValue = imageNode.map(Xpp3Dom::getValue);
    if (imageNameAndTagValue.isPresent()) {
      String imageNameAndTag = imageNameAndTagValue.get();
      int colonIdx = imageNameAndTag.indexOf(':');
      if (colonIdx == -1) {
        imageName = imageNameAndTag;
        // imageTag not specified
      } else {
        imageName = imageNameAndTag.substring(0, colonIdx);
        imageTags.add(imageNameAndTag.substring(colonIdx + 1));
      }

      Optional<Xpp3Dom> tagsNode = toNode.map(to -> to.getChild("tags"));
      Optional<Xpp3Dom[]> tagNodes = tagsNode.map(tags -> tags.getChildren("tag"));
      tagNodes.ifPresent(
          tags -> Arrays.stream(tags).map(Xpp3Dom::getValue).forEach(imageTags::add));

      if (imageTags.isEmpty()) {
        imageTags.add(executionEvent.getProject().getVersion());
      }
      spanBuilder.setAttribute(
          MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME, imageName);
      spanBuilder.setAttribute(
          MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS, imageTags);

      // REGISTRY URL
      String registryHostname =
          imageName.indexOf('/') == -1
              ? "docker.io"
              : imageName.substring(0, imageName.indexOf('/'));

      spanBuilder.setAttribute(
          MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL,
          "https://" + registryHostname);
      spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, "https://" + registryHostname);
      spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
      spanBuilder.setAttribute(SemanticAttributes.PEER_SERVICE, registryHostname);

      // REGISTRY USERNAME
      Optional<Xpp3Dom> authNode = toNode.map(to -> to.getChild("auth"));

      Optional<String> usernameValue =
          authNode.map(auth -> auth.getChild("username")).map(Xpp3Dom::getValue);
      usernameValue.ifPresent(
          username ->
              spanBuilder.setAttribute(
                  MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_AUTH_USERNAME,
                  username));
    } else {
      logger.info(
          "OpenTelemetry: missing element: " + "plugin: " + pluginNode + ", image: " + toNode);
      // FIXME invalid state
    }
  }
}
