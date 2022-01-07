/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
@SuppressWarnings({"DeduplicateConstants", "deprecation"})
public class MojoGoalExecutionHandlerTest {

  @Test
  public void testMavenDeploy() throws Exception {

    String pomXmlPath = "projects/jar/pom.xml";
    String mojoGroupId = "org.apache.maven.plugins";
    String mojoArtifactId = "maven-deploy-plugin";
    String mojoVersion = "2.8.2";
    String mojoGoal = "deploy";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    MavenDeployHandler mavenDeployHandler = new MavenDeployHandler();

    List<MavenGoal> supportedGoals = mavenDeployHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test-tracer").build().spanBuilder("deploy");

      mavenDeployHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_REPOSITORY_ID))
          .isEqualTo("snapshots");
      assertThat(span.getAttribute(SemanticAttributes.HTTP_METHOD)).isEqualTo("POST");
      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL))
          .isEqualTo(
              "https://maven.example.com/repository/maven-snapshots/io/opentelemetry/contrib/maven/test-jar/1.0-SNAPSHOT");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_REPOSITORY_URL))
          .isEqualTo("https://maven.example.com/repository/maven-snapshots/");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("maven.example.com");

      assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);
    }
  }

  @Test
  public void testSpringBootBuildImage_springboot_1() throws Exception {

    String pomXmlPath = "projects/springboot_1/pom.xml";
    String mojoGroupId = "org.springframework.boot";
    String mojoArtifactId = "spring-boot-maven-plugin";
    String mojoVersion = "2.5.6";
    String mojoGoal = "build-image";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    SpringBootBuildImageHandler buildImageHandler = new SpringBootBuildImageHandler();

    List<MavenGoal> supportedGoals = buildImageHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider
              .tracerBuilder("test-tracer")
              .build()
              .spanBuilder("spring-boot:build-image");

      buildImageHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      // TODO improve the Maven test harness that interpolates the maven properties like
      // ${project.artifactId}
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME))
          .isEqualTo("docker.io/john/${project.artifactId}");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS))
          .isEqualTo(Collections.singletonList("latest"));

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL)).isEqualTo("https://docker.io");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("docker.io");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL))
          .isEqualTo("https://docker.io");
    }
  }

  @Test
  public void testSpringBootBuildImage_springboot_2() throws Exception {

    String pomXmlPath = "projects/springboot_2/pom.xml";
    String mojoGroupId = "org.springframework.boot";
    String mojoArtifactId = "spring-boot-maven-plugin";
    String mojoVersion = "2.5.6";
    String mojoGoal = "build-image";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    SpringBootBuildImageHandler buildImageHandler = new SpringBootBuildImageHandler();

    List<MavenGoal> supportedGoals = buildImageHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider
              .tracerBuilder("test-tracer")
              .build()
              .spanBuilder("spring-boot:build-image");

      buildImageHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      // TODO improve the Maven test harness that interpolates the maven properties like
      // ${project.artifactId}
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME))
          .isEqualTo("docker.io/john/${project.artifactId}");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS))
          .isEqualTo(Collections.singletonList("${project.version}"));

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL)).isEqualTo("https://docker.io");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("docker.io");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL))
          .isEqualTo("https://docker.io");
    }
  }

  @Test
  public void testGoogleJibBuild_jib_1() throws Exception {

    String pomXmlPath = "projects/jib_1/pom.xml";
    String mojoGroupId = "com.google.cloud.tools";
    String mojoArtifactId = "jib-maven-plugin";
    String mojoVersion = "3.1.4";
    String mojoGoal = "build";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    GoogleJibBuildHandler buildImageHandler = new GoogleJibBuildHandler();

    List<MavenGoal> supportedGoals = buildImageHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test-tracer").build().spanBuilder("jib:build");

      buildImageHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      // TODO improve the Maven test harness that interpolates the maven properties like
      // ${project.artifactId}
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME))
          .isEqualTo("docker.io/john/${project.artifactId}");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS))
          .isEqualTo(Arrays.asList("latest", "${project.version}"));

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL)).isEqualTo("https://docker.io");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("docker.io");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL))
          .isEqualTo("https://docker.io");
    }
  }

  @Test
  public void testGoogleJibBuild_jib_2() throws Exception {

    String pomXmlPath = "projects/jib_2/pom.xml";
    String mojoGroupId = "com.google.cloud.tools";
    String mojoArtifactId = "jib-maven-plugin";
    String mojoVersion = "3.1.4";
    String mojoGoal = "build";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    GoogleJibBuildHandler buildImageHandler = new GoogleJibBuildHandler();

    List<MavenGoal> supportedGoals = buildImageHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test-tracer").build().spanBuilder("jib:build");

      buildImageHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      // TODO improve the Maven test harness that interpolates the maven properties like
      // ${project.artifactId}
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_NAME))
          .isEqualTo("gcr.io/my-gcp-project/my-app");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_IMAGE_TAGS))
          .isEqualTo(Collections.singletonList("1.0-SNAPSHOT"));

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL)).isEqualTo("https://gcr.io");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("gcr.io");
      assertThat(span.getAttribute(MavenOtelSemanticAttributes.MAVEN_BUILD_CONTAINER_REGISTRY_URL))
          .isEqualTo("https://gcr.io");
    }
  }

  @Test
  public void testSnykTest_snyk_1() throws Exception {

    String pomXmlPath = "projects/snyk_1/pom.xml";
    String mojoGroupId = "io.snyk";
    String mojoArtifactId = "snyk-maven-plugin";
    String mojoVersion = "2.0.0";
    String mojoGoal = "test";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    SnykTestHandler snykTestHandler = new SnykTestHandler();

    List<MavenGoal> supportedGoals = snykTestHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test-tracer").build().spanBuilder("snyk:test");

      snykTestHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL))
          .isEqualTo("https://snyk.io/api/v1/test-dep-graph");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("snyk.io");
      assertThat(span.getAttribute(SemanticAttributes.RPC_METHOD)).isEqualTo("test");
    }
  }

  @Test
  public void testSnykMonitor_snyk_1() throws Exception {

    String pomXmlPath = "projects/snyk_1/pom.xml";
    String mojoGroupId = "io.snyk";
    String mojoArtifactId = "snyk-maven-plugin";
    String mojoVersion = "2.0.0";
    String mojoGoal = "monitor";

    MavenProject project = newMavenProject(pomXmlPath);
    ExecutionEvent executionEvent =
        newMojoStartedExecutionEvent(project, mojoGroupId, mojoArtifactId, mojoVersion, mojoGoal);

    SnykMonitorHandler snykTestHandler = new SnykMonitorHandler();

    List<MavenGoal> supportedGoals = snykTestHandler.getSupportedGoals();
    assertThat(supportedGoals)
        .isEqualTo(
            Collections.singletonList(MavenGoal.create(mojoGroupId, mojoArtifactId, mojoGoal)));

    try (SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build()) {
      SpanBuilder spanBuilder =
          sdkTracerProvider.tracerBuilder("test-tracer").build().spanBuilder("snyk:monitor");

      snykTestHandler.enrichSpan(spanBuilder, executionEvent);
      ReadableSpan span = (ReadableSpan) spanBuilder.startSpan();

      assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);

      assertThat(span.getAttribute(SemanticAttributes.HTTP_URL))
          .isEqualTo("https://snyk.io/api/v1/monitor/maven");
      assertThat(span.getAttribute(SemanticAttributes.PEER_SERVICE)).isEqualTo("snyk.io");
      assertThat(span.getAttribute(SemanticAttributes.RPC_METHOD)).isEqualTo("monitor");
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
    ExecutionEvent.Type type;
    MavenProject project;
    MavenSession session;
    MojoExecution mojoExecution;

    public MockExecutionEvent(
        ExecutionEvent.Type type,
        MavenProject project,
        MavenSession session,
        MojoExecution mojoExecution) {
      this.type = type;
      this.project = project;
      this.session = session;
      this.mojoExecution = mojoExecution;
    }

    @Override
    public ExecutionEvent.Type getType() {
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
