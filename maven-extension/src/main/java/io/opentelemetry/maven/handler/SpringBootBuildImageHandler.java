/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
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
public class SpringBootBuildImageHandler extends AbstractMojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(SpringBootBuildImageHandler.class);

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.springframework.boot", "spring-boot-maven-plugin", "build-image"));
  }

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    super.enrichSpan(spanBuilder, executionEvent);

    spanBuilder.setSpanKind(SpanKind.CLIENT);

    Xpp3Dom pluginCfg = executionEvent.getMojoExecution().getConfiguration();
    Xpp3Dom dockerImageCfg = pluginCfg == null ? null : pluginCfg.getChild("image");
    String imageName;
    String imageTag;
    if (dockerImageCfg == null || dockerImageCfg.getChild("name") == null) {
      // see
      // https://docs.spring.io/spring-boot/docs/2.6.1/maven-plugin/reference/htmlsingle/#build-image.customization
      // default image name docker.io/library/${project.artifactId}:${project.version}
      imageName = "docker.io/library/" + executionEvent.getProject().getArtifactId();
      imageTag = executionEvent.getProject().getVersion();
    } else {
      imageName = dockerImageCfg.getChild("name").getValue();
      int colonIdx = imageName.indexOf(':');
      if (colonIdx == -1) {
        imageTag = "latest";
      } else {
        imageTag = imageName.substring(colonIdx);
        imageName = imageName.substring(0, colonIdx);
      }
    }
    // FIXME handle use cases when additional additional `tags` are provided
    spanBuilder.setAttribute(ResourceAttributes.CONTAINER_IMAGE_NAME, imageName);
    spanBuilder.setAttribute(ResourceAttributes.CONTAINER_IMAGE_TAG, imageTag);

    if (pluginCfg != null
        && pluginCfg.getChild("publish") != null
        && Boolean.parseBoolean(pluginCfg.getChild("publish").getValue())) {
      Xpp3Dom dockerCfg = pluginCfg.getChild("docker");
      Xpp3Dom publishRegistryCfg = dockerCfg == null ? null : dockerCfg.getChild("publishRegistry");

      // REGISTRY URL
      Xpp3Dom registryUrlCfg =
          publishRegistryCfg == null ? null : publishRegistryCfg.getChild("url");

      String url = registryUrlCfg == null ? null : registryUrlCfg.getValue();
      if (url == null) {
      } else if (url.startsWith("http://") || url.startsWith("https://")) {
        try {
          URL containerRegistryUrl = new URL(url);
          // Note that setting the net_peer_service helps visualization but
          // doesn't fully comply with the Otel spec
          spanBuilder.setAttribute(SemanticAttributes.PEER_SERVICE, containerRegistryUrl.getHost());
        } catch (MalformedURLException e) {
          logger.debug("Ignore exception parsing container registry URL", e);
        }

        spanBuilder.setAttribute(MavenOtelSemanticAttributes.CONTAINER_REGISTRY_URL, url);
        spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, url);
        spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
      }
      // REGISTRY USERNAME
      Xpp3Dom registryUsernameCfg =
          publishRegistryCfg == null ? null : publishRegistryCfg.getChild("username");
      if (registryUsernameCfg != null) {
        spanBuilder.setAttribute(
            MavenOtelSemanticAttributes.CONTAINER_REGISTRY_USERNAME,
            registryUsernameCfg.getValue());
      }
    }
  }
}
