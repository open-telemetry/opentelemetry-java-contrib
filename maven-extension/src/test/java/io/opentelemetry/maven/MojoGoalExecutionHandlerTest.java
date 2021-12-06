/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.handler.DeployDeployHandler;
import io.opentelemetry.maven.handler.SpringBootBuildImageHandler;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

/**
 * TODO Find a better solution to instantiate a MavenProject and a MojoExecutionEvent. See
 * https://github.com/takari/takari-lifecycle/blob/master/takari-lifecycle-plugin/src/test/java/io/takari/maven/plugins/plugin/PluginDescriptorMojoTest.java
 */
public class MojoGoalExecutionHandlerTest {

  @Test
  public void testDeployDeploy() throws Exception {

    String pomXmlPath = "projects/jar/pom.xml";
    String mojoGroupId = "org.apache.maven.plugins";
    String mojoArtifactId = "maven-deploy-plugin";
    String mojoVersion = "2.8.2";
    String mojoGoal = "deploy";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    DeployDeployHandler deployDeployHandler = new DeployDeployHandler();

    boolean actual = deployDeployHandler.supports(executionEvent);
    assertThat(actual).isEqualTo(true);

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test").build().spanBuilder("deploy");

      deployDeployHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_REPOSITORY_ID))
          .isEqualTo("snapshots");
      assertThat(span.getAttribute(SemanticAttributes.HTTP_METHOD)).isEqualTo("POST");
      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL))
          .isEqualTo(
              "https://maven.example.com/repository/maven-snapshots/io/opentelemetry/contrib/maven/test-jar/1.0-SNAPSHOT");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_REPOSITORY_URL))
          .isEqualTo("https://maven.example.com/repository/maven-snapshots/");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("maven.example.com");

      assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);
    }
  }

  @Test
  public void testSpringBootBuildImage() throws Exception {

    String pomXmlPath = "projects/springboot/pom.xml";
    String mojoGroupId = "org.springframework.boot";
    String mojoArtifactId = "spring-boot-maven-plugin";
    String mojoVersion = "2.5.6";
    String mojoGoal = "build-image";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    SpringBootBuildImageHandler buildImageHandler = new SpringBootBuildImageHandler();

    boolean actual = buildImageHandler.supports(executionEvent);
    assertThat(actual).isEqualTo(true);

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test").build().spanBuilder("spring-boot:build-image");

      buildImageHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      // FIXME need better initialization of the MavenExecutionEvent to test this
      // assertThat(span.getAttribute(ResourceAttributes.CONTAINER_IMAGE_NAME))
      //    .isEqualTo("docker.io/john/springboot-test");
      assertThat(span.getAttribute(ResourceAttributes.CONTAINER_IMAGE_TAG)).isEqualTo("latest");

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL)).isEqualTo("https://docker.io");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("docker.io");
    }
  }

  ExecutionEvent newMojoStartedExecutionEvent(
      MavenProject project,
      String mojoGroupId,
      String mojoArtifactId,
      String mojoVersion,
      String mojoGoal) {

    MojoExecution mojoExecution =
        newMojoExecution(mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal, project);

    MavenSession session =
        new MavenSession(
            null, null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
    session.setCurrentProject(project);

    return new MockExecutionEvent(ExecutionEvent.Type.MojoStarted, project, session, mojoExecution);
  }

  MavenProject newMavenProject(String pomXmlPath)
      throws IOException, XmlPullParserException, InvalidRepositoryException {
    InputStream pomXmlAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(pomXmlPath);

    MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
    Model model = mavenXpp3Reader.read(pomXmlAsStream);

    MavenProject project = new MavenProject(model);
    project.setArtifact(new ProjectArtifact(project));

    DistributionManagement distributionManagement = model.getDistributionManagement();
    if (distributionManagement != null) {
      project.setSnapshotArtifactRepository(
          MavenRepositorySystem.buildArtifactRepository(
              distributionManagement.getSnapshotRepository()));
      project.setReleaseArtifactRepository(
          MavenRepositorySystem.buildArtifactRepository(distributionManagement.getRepository()));
    }
    return project;
  }

  MojoExecution newMojoExecution(
      String groupId, String artifactId, String version, String goal, MavenProject project) {
    PluginDescriptor pluginDescriptor = new PluginDescriptor();
    pluginDescriptor.setGroupId(groupId);
    pluginDescriptor.setArtifactId(artifactId);
    pluginDescriptor.setVersion(version);
    MojoDescriptor mojoDescriptor = new MojoDescriptor();
    mojoDescriptor.setGoal(goal);
    mojoDescriptor.setPluginDescriptor(pluginDescriptor);
    MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);

    Plugin plugin = new Plugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    plugin.setVersion(version);

    final Plugin configuredPlugin = project.getPlugin(plugin.getKey());
    if (configuredPlugin != null) {
      mojoExecution.setConfiguration((Xpp3Dom) configuredPlugin.getConfiguration());
    }
    return mojoExecution;
  }

  static class MockExecutionEvent implements ExecutionEvent {
    Type type;
    MavenProject project;
    MavenSession session;
    MojoExecution mojoExecution;

    public MockExecutionEvent(
        Type type, MavenProject project, MavenSession session, MojoExecution mojoExecution) {
      this.type = type;
      this.project = project;
      this.session = session;
      this.mojoExecution = mojoExecution;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public MavenSession getSession() {
      return session;
    }

    @Override
    public MavenProject getProject() {
      return project;
    }

    @Override
    public MojoExecution getMojoExecution() {
      return mojoExecution;
    }

    @Nullable
    @Override
    public Exception getException() {
      return null;
    }
  }
}
