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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
public class SpringBootBuildImageHandler implements MojoGoalExecutionHandler {
  private static final Logger logger = LoggerFactory.getLogger(SpringBootBuildImageHandler.class);

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(
        MavenGoal.create("org.springframework.boot", "spring-boot-maven-plugin", "build-image"));
  }

  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {

    spanBuilder.setSpanKind(SpanKind.CLIENT);

    Optional<Xpp3Dom> pluginNode =
        Optional.ofNullable(executionEvent.getMojoExecution().getConfiguration());
    Optional<Xpp3Dom> imageNode = pluginNode.map(cfg -> cfg.getChild("image"));
    Optional<String> optImageNameAndTag =
        imageNode.map(image -> image.getChild("name")).map(name -> name.getValue());

    String imageName;
    String imageTag;
    if (optImageNameAndTag.isPresent()) {
      String imageNameAndTag = optImageNameAndTag.get();
      int colonIdx = imageNameAndTag.indexOf(':');
      if (colonIdx == -1) {
        // imageName is unchanged
        imageName = imageNameAndTag;
        imageTag = "latest";
      } else {
        imageTag = imageNameAndTag.substring(colonIdx + 1);
        imageName = imageNameAndTag.substring(0, colonIdx);
      }
    } else {
      // see
      // https://docs.spring.io/spring-boot/docs/2.6.1/maven-plugin/reference/htmlsingle/#build-image.customization
      // default image name docker.io/library/${project.artifactId}:${project.version}
      imageName = "docker.io/library/" + executionEvent.getProject().getArtifactId();
      imageTag = executionEvent.getProject().getVersion();
    }

    // FIXME handle use cases when additional additional `tags` are provided
    // cyrille didn't understand yet from the Spring docs how to define multiple tags
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME, imageName);
    spanBuilder.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS,
        Collections.singletonList(imageTag));

    Optional<Xpp3Dom> publishNode = pluginNode.map(cfg -> cfg.getChild("publish"));
    Optional<Boolean> optPublish =
        publishNode.map(publish -> Boolean.parseBoolean(publish.getValue()));
    if (optPublish.orElse(false)) {
      Optional<Xpp3Dom> dockerNode = pluginNode.map(plugin -> plugin.getChild("docker"));
      Optional<Xpp3Dom> registryNode = dockerNode.map(docker -> docker.getChild("publishRegistry"));

      // REGISTRY URL
      Optional<String> optUrl =
          registryNode
              .map(registry -> registry.getChild("url"))
              .map(registryUrl -> registryUrl.getValue());

      optUrl
          .filter(url -> url.startsWith("http://") || url.startsWith("https://"))
          .ifPresent(
              url -> {
                spanBuilder.setAttribute(MavenOtelSemanticAttributes.CONTAINER_REGISTRY_URL, url);
                spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, url);
                spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
                try {
                  URL containerRegistryUrl = new URL(url);
                  // Note that setting the net_peer_service helps visualization but
                  // doesn't fully comply with the Otel spec
                  spanBuilder.setAttribute(
                      SemanticAttributes.PEER_SERVICE, containerRegistryUrl.getHost());
                } catch (MalformedURLException e) {
                  logger.debug("Ignore exception parsing container registry URL", e);
                }
              });

      // REGISTRY USERNAME
      Optional<String> usernameValue =
          registryNode.map(registry -> registry.getChild("username")).map(Xpp3Dom::getValue);
      usernameValue.ifPresent(
          username ->
              spanBuilder.setAttribute(
                  MavenOtelSemanticAttributes.CONTAINER_REGISTRY_USERNAME, username));
    }
  }
}
