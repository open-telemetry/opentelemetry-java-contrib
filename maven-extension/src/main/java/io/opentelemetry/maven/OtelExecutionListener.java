/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.maven.handler.MojoGoalExecutionHandler;
import io.opentelemetry.maven.handler.MojoGoalExecutionHandlerConfiguration;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.logs.LogEmitter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Close the OpenTelemetry SDK (see {@link OpenTelemetrySdkService#dispose()}) on the end of
 * execution of the last project ({@link #projectSucceeded(ExecutionEvent)} and {@link
 * #projectFailed(ExecutionEvent)}) rather than on the end of the Maven session {@link
 * #sessionEnded(ExecutionEvent)} because OpenTelemetry and GRPC classes are unloaded by the Maven
 * classloader before {@link #sessionEnded(ExecutionEvent)} causing {@link NoClassDefFoundError}
 * messages in the logs.
 */
@Component(role = ExecutionListener.class, hint = "otel-execution-listener")
public final class OtelExecutionListener extends AbstractExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(OtelExecutionListener.class);

  @SuppressWarnings("NullAway") // Automatically initialized by DI
  @Requirement
  private SpanRegistry spanRegistry;

  @SuppressWarnings("NullAway") // Automatically initialized by DI
  @Requirement
  private OpenTelemetrySdkService openTelemetrySdkService;

  private Map<MavenGoal, MojoGoalExecutionHandler> mojoGoalExecutionHandlers = new HashMap<>();

  public OtelExecutionListener() {
    this.mojoGoalExecutionHandlers =
        MojoGoalExecutionHandlerConfiguration.loadMojoGoalExecutionHandler(
            OtelExecutionListener.class.getClassLoader());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "OpenTelemetry: mojoGoalExecutionHandlers: "
              + mojoGoalExecutionHandlers.entrySet().stream()
                  .map(entry -> entry.getKey().toString() + ": " + entry.getValue().toString())
                  .collect(Collectors.joining(", ")));
    }
  }

  static boolean alwaysOutputLogsToStdout = true;
  /**
   * Register in given {@link OtelExecutionListener} to the lifecycle of the given {@link
   * MavenSession}
   *
   * @see org.apache.maven.execution.MavenExecutionRequest#setExecutionListener(ExecutionListener)
   */
  public static void registerOtelExecutionListener(
      MavenSession session, OtelExecutionListener otelExecutionListener) {

    LogEmitter logEmitter = otelExecutionListener.openTelemetrySdkService.getLogEmitter();
    MavenExecutionRequest mavenExecutionRequest = session.getRequest();
    ExecutionListener previousExecutionListener = mavenExecutionRequest.getExecutionListener();

    List<ExecutionListener> newExecutionListeners = new LinkedList<>();
    if (previousExecutionListener instanceof ChainedExecutionListener
        || previousExecutionListener instanceof OtelExecutionListener) {
      // Otel already initialized
      newExecutionListeners.add(previousExecutionListener);
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension already registered as execution listener, skip.");
    } else {
      newExecutionListeners.add(otelExecutionListener);

      if (logEmitter == null) {
        if (previousExecutionListener != null) {
          newExecutionListeners.add(previousExecutionListener);
        }
      } else {
        if (previousExecutionListener == null) {
          // nothing to do here
        } else if (!alwaysOutputLogsToStdout
            && "org.apache.maven.cli.event.ExecutionEventLogger"
                .equals(previousExecutionListener.getClass().getName())) {
          // we replace the existing log listener by Otel logs se we don't add the previous log
          // listener
        } else {
          newExecutionListeners.add(previousExecutionListener);
        }
      }

      ExecutionListener newExecutionListener =
          newExecutionListeners.size() == 1
              ? newExecutionListeners.get(0)
              : new ChainedExecutionListener(newExecutionListeners);
      mavenExecutionRequest.setExecutionListener(newExecutionListener);
      logger.info( // FIXME REVERT LOG LEVEL TO DEBUG
          "OpenTelemetry: OpenTelemetry extension registered as execution listener {}. InitialExecutionListener: {}",
          newExecutionListener,
          previousExecutionListener);
    }
  }

  @Override
  public void sessionStarted(ExecutionEvent executionEvent) {
    MavenProject project = executionEvent.getSession().getTopLevelProject();
    TextMapGetter<Map<String, String>> toUpperCaseTextMapGetter = new ToUpperCaseTextMapGetter();
    io.opentelemetry.context.Context context =
        openTelemetrySdkService
            .getPropagators()
            .getTextMapPropagator()
            .extract(
                io.opentelemetry.context.Context.current(),
                System.getenv(),
                toUpperCaseTextMapGetter);

    // TODO question: is this the root span name we want?
    // It's interesting for the root span name to
    // - start with an operation name: "Build"
    // - identify the build with a popular identifier of what is being built, in the java culture,
    //   it's the artifact identifier
    String spanName =
        "Build: "
            + project.getGroupId()
            + ":"
            + project.getArtifactId()
            + ":"
            + project.getVersion();
    logger.debug("OpenTelemetry: Start session span: {}", spanName);
    Span sessionSpan =
        this.openTelemetrySdkService
            .getTracer()
            .spanBuilder(spanName)
            .setParent(context)
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID, project.getGroupId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID, project.getArtifactId())
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION, project.getVersion())
            .setSpanKind(SpanKind.SERVER)
            .startSpan();
    spanRegistry.setRootSpan(sessionSpan);
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.sessionStarted(executionEvent, logEmitter, sessionSpan);
    }
  }

  @Override
  public void projectStarted(ExecutionEvent executionEvent) {
    MavenProject project = executionEvent.getProject();
    String spanName = project.getGroupId() + ":" + project.getArtifactId();
    logger.debug("OpenTelemetry: Start project span: {}", spanName);
    Span rootSpan = spanRegistry.getRootSpanNotNull();
    Tracer tracer = this.openTelemetrySdkService.getTracer();
    Span projectSpan =
        tracer
            .spanBuilder(spanName)
            .setParent(Context.current().with(Span.wrap(rootSpan.getSpanContext())))
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID, project.getGroupId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID, project.getArtifactId())
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION, project.getVersion())
            .startSpan();
    spanRegistry.putSpan(projectSpan, project);
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.projectStarted(executionEvent, logEmitter, projectSpan);
    }
  }

  @Override
  public void projectSucceeded(ExecutionEvent executionEvent) {
    logger.debug(
        "OpenTelemetry: End succeeded project span: {}:{}",
        executionEvent.getProject().getArtifactId(),
        executionEvent.getProject().getArtifactId());
    spanRegistry.removeSpan(executionEvent.getProject()).end();
  }

  @Override
  public void projectFailed(ExecutionEvent executionEvent) {
    logger.debug(
        "OpenTelemetry: End failed project span: {}:{}",
        executionEvent.getProject().getArtifactId(),
        executionEvent.getProject().getArtifactId());
    Span span = spanRegistry.removeSpan(executionEvent.getProject());
    span.setStatus(StatusCode.ERROR);
    span.recordException(executionEvent.getException());
    span.end();
  }

  @Override
  public void mojoStarted(ExecutionEvent executionEvent) {
    if (!this.openTelemetrySdkService.isMojosInstrumentationEnabled()) {
      return;
    }

    MojoExecution mojoExecution = executionEvent.getMojoExecution();

    Span rootSpan = spanRegistry.getSpan(executionEvent.getProject());

    String spanName =
        MavenUtils.getPluginArtifactIdShortName(mojoExecution.getArtifactId())
            + ":"
            + mojoExecution.getGoal();
    logger.debug("OpenTelemetry: Start mojo execution: span {}", spanName);
    SpanBuilder spanBuilder =
        this.openTelemetrySdkService
            .getTracer()
            .spanBuilder(spanName)
            .setParent(Context.current().with(Span.wrap(rootSpan.getSpanContext())))
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PROJECT_GROUP_ID,
                executionEvent.getProject().getGroupId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PROJECT_ARTIFACT_ID,
                executionEvent.getProject().getArtifactId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PROJECT_VERSION,
                executionEvent.getProject().getVersion())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PLUGIN_GROUP_ID,
                mojoExecution.getPlugin().getGroupId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PLUGIN_ARTIFACT_ID,
                mojoExecution.getPlugin().getArtifactId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_PLUGIN_VERSION,
                mojoExecution.getPlugin().getVersion())
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_EXECUTION_GOAL, mojoExecution.getGoal())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_EXECUTION_ID, mojoExecution.getExecutionId())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_EXECUTION_LIFECYCLE_PHASE,
                mojoExecution.getLifecyclePhase());
    //  enrich spans with MojoGoalExecutionHandler
    MojoGoalExecutionHandler handler =
        this.mojoGoalExecutionHandlers.get(MavenGoal.create(mojoExecution));
    logger.debug("OpenTelemetry: {} handler {}", executionEvent, handler);
    if (handler != null) {
      handler.enrichSpan(spanBuilder, executionEvent);
    }

    Span span = spanBuilder.startSpan();
    spanRegistry.putSpan(span, mojoExecution, executionEvent.getProject());
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.mojoStarted(executionEvent, logEmitter, span);
    }
  }

  @Override
  public void mojoSucceeded(ExecutionEvent executionEvent) {
    if (!this.openTelemetrySdkService.isMojosInstrumentationEnabled()) {
      return;
    }
    MojoExecution mojoExecution = executionEvent.getMojoExecution();
    logger.debug(
        "OpenTelemetry: End succeeded mojo execution span: {}, {}",
        mojoExecution,
        executionEvent.getProject());
    Span mojoExecutionSpan = spanRegistry.removeSpan(mojoExecution, executionEvent.getProject());
    mojoExecutionSpan.setStatus(StatusCode.OK);

    mojoExecutionSpan.end();
  }

  @Override
  public void mojoFailed(ExecutionEvent executionEvent) {
    if (!this.openTelemetrySdkService.isMojosInstrumentationEnabled()) {
      return;
    }
    MojoExecution mojoExecution = executionEvent.getMojoExecution();
    logger.debug(
        "OpenTelemetry: End failed mojo execution span: {}, {}",
        mojoExecution,
        executionEvent.getProject());
    Span mojoExecutionSpan = spanRegistry.removeSpan(mojoExecution, executionEvent.getProject());
    mojoExecutionSpan.setStatus(StatusCode.ERROR, "Mojo Failed"); // TODO verify description
    Throwable exception = executionEvent.getException();
    if (exception instanceof LifecycleExecutionException) {
      LifecycleExecutionException executionException = (LifecycleExecutionException) exception;
      // we already capture the context, no need to capture it again
      exception = executionException.getCause();
    }
    mojoExecutionSpan.recordException(exception);
    mojoExecutionSpan.end();
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    logger.debug("OpenTelemetry: Maven session ended");
    Span span = spanRegistry.removeRootSpan();
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.sessionEnded(event, logEmitter, span);
    }
    span.end();
  }

  @Override
  public void projectDiscoveryStarted(ExecutionEvent event) {
    // session span not created, skip
  }

  @Override
  public void projectSkipped(ExecutionEvent event) {
    Span sessionSpan = spanRegistry.getRootSpanNotNull();
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.projectSkipped(event, logEmitter, sessionSpan);
    }
  }

  @Override
  public void mojoSkipped(ExecutionEvent event) {
    Span span = spanRegistry.getSpan(event.getProject());
    LogEmitter logEmitter = openTelemetrySdkService.getLogEmitter();
    if (logEmitter != null) {
      OtelLogsExecutionListener.mojoSkipped(event, logEmitter, span);
    }
  }

  private static class ToUpperCaseTextMapGetter implements TextMapGetter<Map<String, String>> {
    @Override
    public Iterable<String> keys(Map<String, String> environmentVariables) {
      return environmentVariables.keySet();
    }

    @Override
    @Nullable
    public String get(@Nullable Map<String, String> environmentVariables, String key) {
      return environmentVariables == null
          ? null
          : environmentVariables.get(key.toUpperCase(Locale.ROOT));
    }
  }
}
