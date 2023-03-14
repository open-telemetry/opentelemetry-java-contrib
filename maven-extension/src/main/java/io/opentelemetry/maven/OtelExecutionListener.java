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
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.maven.handler.MojoGoalExecutionHandler;
import io.opentelemetry.maven.handler.MojoGoalExecutionHandlerConfiguration;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
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

  /**
   * Note that using a thread local around the mojo goal execution to carry the {@link Scope }
   * works even when using Maven build parallelization.
   * {@link Span#current()} invoked in {@link org.apache.maven.plugin.Mojo#execute()} returns as
   * expected the span set in {@link ExecutionListener#mojoStarted(ExecutionEvent)} using
   * {@link Span#makeCurrent()}. For this reason, we can carry over the {@link Scope} in a thread
   * local variable.
   */
  private static final ThreadLocal<Scope> MOJO_EXECUTION_SCOPE = new ThreadLocal<>();

  @SuppressWarnings("NullAway") // Automatically initialized by DI
  @Requirement
  private SpanRegistry spanRegistry;

  @SuppressWarnings("NullAway") // Automatically initialized by DI
  @Requirement
  private OpenTelemetrySdkService openTelemetrySdkService;

  private final Map<MavenGoal, MojoGoalExecutionHandler> mojoGoalExecutionHandlers;

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

      // help debugging class loader issues when the OTel APIs used in
      // Maven plugin mojos are mistakenly not loaded by the OTel Maven extension
      // causing the lack of context propagation from the OTel Maven extension to the plugin mojos
      ContextStorage contextStorage = ContextStorage.get();
      logger.debug(
          "ContextStorage: "
              + contextStorage
              + ", identity="
              + System.identityHashCode(contextStorage));
      Class<? extends ContextStorage> contextStorageClass = contextStorage.getClass();
      logger.debug(
          "ContextStorageClass="
              + contextStorageClass.getName()
              + ", identity="
              + System.identityHashCode(contextStorageClass)
              + " classloader="
              + contextStorageClass.getClassLoader()
              + " codeLocation="
              + Optional.of(contextStorageClass.getProtectionDomain().getCodeSource())
                  .map(source -> source.getLocation().toString())
                  .orElse("#unknown#"));
    }
  }

  /**
   * Register in given {@link OtelExecutionListener} to the lifecycle of the given {@link
   * MavenSession}
   *
   * @see org.apache.maven.execution.MavenExecutionRequest#setExecutionListener(ExecutionListener)
   */
  public static void registerOtelExecutionListener(
      MavenSession session, OtelExecutionListener otelExecutionListener) {

    ExecutionListener initialExecutionListener = session.getRequest().getExecutionListener();
    if (initialExecutionListener instanceof ChainedExecutionListener
        || initialExecutionListener instanceof OtelExecutionListener) {
      // already initialized
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension already registered as execution listener, skip.");
    } else if (initialExecutionListener == null) {
      session.getRequest().setExecutionListener(otelExecutionListener);
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as execution listener. No execution listener initially defined");
    } else {
      session
          .getRequest()
          .setExecutionListener(
              new ChainedExecutionListener(otelExecutionListener, initialExecutionListener));
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as execution listener. InitialExecutionListener: "
              + initialExecutionListener);
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
    @SuppressWarnings("MustBeClosedChecker")
    Scope scope = span.makeCurrent();
    spanRegistry.putSpan(span, mojoExecution, executionEvent.getProject());
    Optional.ofNullable(MOJO_EXECUTION_SCOPE.get())
        .ifPresent(
            previousScope ->
                logger.warn(
                    "OpenTelemetry: Scope "
                        + System.identityHashCode(previousScope)
                        + "already attached to thread '"
                        + Thread.currentThread().getName()
                        + "'"));
    MOJO_EXECUTION_SCOPE.set(scope);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "OpenTelemetry: Attach scope "
              + System.identityHashCode(scope)
              + " to thread '"
              + Thread.currentThread().getName()
              + "' for "
              + mojoExecution);
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
    Scope scope = MOJO_EXECUTION_SCOPE.get();
    if (scope == null) {
      logger.warn(
          "OpenTelemetry: No scope found on thread '"
              + Thread.currentThread().getName()
              + "' for succeeded "
              + mojoExecution);
    } else {
      scope.close();
      MOJO_EXECUTION_SCOPE.remove();
      if (logger.isDebugEnabled()) {
        logger.debug(
            "OpenTelemetry: Remove scope "
                + System.identityHashCode(scope)
                + " on thread '"
                + Thread.currentThread().getName()
                + "' for succeeded "
                + mojoExecution);
      }
    }
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
    if (exception != null) {
      mojoExecutionSpan.recordException(exception);
    }
    mojoExecutionSpan.end();
    Scope scope = MOJO_EXECUTION_SCOPE.get();
    if (scope == null) {
      logger.warn(
          "OpenTelemetry: No scope found on thread '"
              + Thread.currentThread().getName()
              + "' for failed "
              + mojoExecution);
    } else {
      scope.close();
      MOJO_EXECUTION_SCOPE.remove();
      if (logger.isDebugEnabled()) {
        logger.debug(
            "OpenTelemetry: Remove scope "
                + System.identityHashCode(scope)
                + " on thread '"
                + Thread.currentThread().getName()
                + "' for failed "
                + mojoExecution);
      }
    }
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    logger.debug("OpenTelemetry: Maven session ended");
    spanRegistry.removeRootSpan().end();
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
