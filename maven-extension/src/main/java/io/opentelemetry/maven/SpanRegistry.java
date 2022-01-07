/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.trace.Span;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hold the state of the spans in progress.
 *
 * <p>As Maven processes, including the <a href="https://github.com/apache/maven-mvnd">Maven
 * Daemon</a>, can't execute multiple builds concurrently, there is no need to differentiate spans
 * per {@link org.apache.maven.execution.MavenSession}.
 */
@Component(role = SpanRegistry.class)
public final class SpanRegistry {

  private static final Logger logger = LoggerFactory.getLogger(SpanRegistry.class);

  private final Map<MojoExecutionKey, Span> mojoExecutionKeySpanMap = new ConcurrentHashMap<>();
  private final Map<MavenProjectKey, Span> mavenProjectKeySpanMap = new ConcurrentHashMap<>();
  @Nullable private Span rootSpan;

  /**
   * Sets the root span.
   *
   * @throws IllegalStateException Root span already defined
   */
  public void setRootSpan(Span rootSpan) {
    if (this.rootSpan != null) {
      throw new IllegalStateException("Root span already defined " + this.rootSpan);
    }
    this.rootSpan = rootSpan;
  }

  public Span getSpan(MavenProject mavenProject) {
    logger.debug("OpenTelemetry: getSpan({}, {})", mavenProject, Thread.currentThread());
    final MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
    final Span span = this.mavenProjectKeySpanMap.get(key);
    if (span == null) {
      throw new IllegalStateException(
          "Span not started for project "
              + mavenProject.getGroupId()
              + ":"
              + mavenProject.getArtifactId());
    }
    return span;
  }

  public Span getRootSpanNotNull() {
    Span rootSpan = this.rootSpan;
    if (rootSpan == null) {
      throw new IllegalStateException("Root span not defined");
    }
    return rootSpan;
  }

  public Span removeRootSpan() {
    Span rootSpan = this.rootSpan;
    if (rootSpan == null) {
      throw new IllegalStateException("Root span not defined");
    }
    if (!this.mojoExecutionKeySpanMap.isEmpty()) {
      throw new IllegalStateException(
          "Remaining children spans: "
              + this.mojoExecutionKeySpanMap.keySet().stream()
                  .map(MojoExecutionKey::toString)
                  .collect(Collectors.joining(", ")));
    }
    this.rootSpan = null;
    return rootSpan;
  }

  public void putSpan(Span span, MavenProject mavenProject) {
    logger.debug("OpenTelemetry: putSpan({})", mavenProject);
    MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
    Span previousSpanForKey = mavenProjectKeySpanMap.put(key, span);
    if (previousSpanForKey != null) {
      throw new IllegalStateException("A span has already been started for " + mavenProject);
    }
  }

  public void putSpan(Span span, MojoExecution mojoExecution, MavenProject project) {
    logger.debug("OpenTelemetry: putSpan({}, {})", mojoExecution, project);
    MojoExecutionKey key = MojoExecutionKey.fromMojoExecution(mojoExecution, project);
    Span previousSpanForKey = mojoExecutionKeySpanMap.put(key, span);
    if (previousSpanForKey != null) {
      throw new IllegalStateException(
          "A span has already been started for " + mojoExecution + ", " + project);
    }
  }

  public Span removeSpan(MavenProject mavenProject) {
    logger.debug("OpenTelemetry: removeSpan({})", mavenProject);
    MavenProjectKey key = MavenProjectKey.fromMavenProject(mavenProject);
    Span span = mavenProjectKeySpanMap.remove(key);
    if (span == null) {
      throw new IllegalStateException("No span found for " + mavenProject);
    }
    return span;
  }

  @Nonnull
  public Span removeSpan(MojoExecution mojoExecution, MavenProject project) {
    logger.debug("OpenTelemetry: removeSpan({}, {})", mojoExecution, project);
    MojoExecutionKey key = MojoExecutionKey.fromMojoExecution(mojoExecution, project);
    Span span = mojoExecutionKeySpanMap.remove(key);
    if (span == null) {
      throw new IllegalStateException("No span found for " + mojoExecution + " " + project);
    }
    return span;
  }

  @AutoValue
  abstract static class MavenProjectKey {
    abstract String groupId();

    abstract String artifactId();

    abstract String version();

    public static MavenProjectKey fromMavenProject(@Nonnull MavenProject project) {
      return new AutoValue_SpanRegistry_MavenProjectKey(
          project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
  }

  @AutoValue
  abstract static class MojoExecutionKey {
    abstract String executionId();

    abstract String goal();

    abstract String groupId();

    abstract String artifactId();

    abstract String pluginGroupId();

    abstract String pluginArtifactId();

    abstract MavenProjectKey projectKey();

    static MojoExecutionKey fromMojoExecution(MojoExecution mojoExecution, MavenProject project) {
      if (mojoExecution == null) {
        throw new NullPointerException("Given MojoExecution is null");
      }
      Plugin plugin = mojoExecution.getPlugin();
      if (plugin == null) {
        throw new NullPointerException(
            "Plugin is null for MojoExecution " + mojoExecution.identify());
      }
      return new AutoValue_SpanRegistry_MojoExecutionKey(
          mojoExecution.getExecutionId(),
          mojoExecution.getGoal(),
          mojoExecution.getGroupId(),
          mojoExecution.getArtifactId(),
          plugin.getGroupId(),
          plugin.getArtifactId(),
          MavenProjectKey.fromMavenProject(project));
    }
  }
}
