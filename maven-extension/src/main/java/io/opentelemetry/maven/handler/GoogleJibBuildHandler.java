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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** See https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin */
final class GoogleJibBuildHandler implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GoogleJibBuildHandler.class);

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("com.google.cloud.tools", "jib-maven-plugin", "build"));
  }

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);

    Xpp3Dom pluginNode = executionEvent.getMojoExecution().getConfiguration();
    if (pluginNode == null) {
      logger.debug("OpenTelemetry: GoogleJibBuildHandler: config node not found");
      return;
    }
    Xpp3Dom toNode = pluginNode.getChild("to");
    if (pluginNode == null) {
      logger.debug("OpenTelemetry: GoogleJibBuildHandler: 'to' node not found");
      return;
    }
    Xpp3Dom imageNode = toNode.getChild("image");
    if (pluginNode == null) {
      logger.debug("OpenTelemetry: GoogleJibBuildHandler: 'to/image' node not found");
      return;
    }
    String imageNameAndTagValue = imageNode.getValue();
    if (imageNameAndTagValue == null) {
      logger.debug("OpenTelemetry: GoogleJibBuildHandler: value of node 'to/image' is null");
      return;
    }

    String imageName;
    List<String> imageTags = new ArrayList<>();

    int colonIdx = imageNameAndTagValue.indexOf(':');
    if (colonIdx == -1) {
      imageName = imageNameAndTagValue;
      // imageTag not specified
    } else {
      imageName = imageNameAndTagValue.substring(0, colonIdx);
      imageTags.add(imageNameAndTagValue.substring(colonIdx + 1));
    }

    Xpp3Dom tagsNode = toNode.getChild("tags");
    Xpp3Dom[] tagNodes = tagsNode == null ? new Xpp3Dom[0] : tagsNode.getChildren("tag");
    Arrays.stream(tagNodes).map(Xpp3Dom::getValue).forEach(imageTags::add);

    if (imageTags.isEmpty()) {
      // default value
      imageTags.add(executionEvent.getProject().getVersion());
    }
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME, imageName);
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS, imageTags);

    // REGISTRY URL
    String registryHostname =
        imageName.indexOf('/') == -1 ? "docker.io" : imageName.substring(0, imageName.indexOf('/'));

    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL,
        "https://" + registryHostname);
    spanBuilder.setAttribute(UrlAttributes.URL_FULL, "https://" + registryHostname);
    spanBuilder.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST");
    // Note: setting the "peer.service" helps visualization on Jaeger but
    // may not fully comply with the OTel "peer.service" spec as we don't know if the remote
    // service will be instrumented and what it "service.name" would be
    spanBuilder.setAttribute(MavenOtelSemanticAttributes.PEER_SERVICE, registryHostname);
  }
}
