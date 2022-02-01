/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogEmitter;
import io.opentelemetry.sdk.logs.data.Severity;
import java.time.Duration;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.codehaus.plexus.util.StringUtils;

/** Inspired by {@code org.apache.maven.cli.event.ExecutionEventLogger} */
public class OtelLogsExecutionListener {
  private OtelLogsExecutionListener() {}

  private static final int LINE_LENGTH = 72;
  private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;
  private static final int MAX_PROJECT_NAME_LENGTH = 52;

  private static String chars(char c, int count) {
    StringBuilder buffer = new StringBuilder(count);

    for (int i = count; i > 0; i--) {
      buffer.append(c);
    }

    return buffer.toString();
  }

  static void projectDiscoveryStarted(LogEmitter logEmitter, Span span) {
    emitLog("Scanning for projects...", Severity.INFO, logEmitter, span);
  }

  static void sessionStarted(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    if (event.getSession().getProjects().size() > 1) {
      MessageBuilder message = buffer();
      message
          .strong(chars('-', LINE_LENGTH))
          .newline()
          .strong("Reactor Build Order:")
          .newline()
          .newline();
      for (MavenProject project : event.getSession().getProjects()) {
        message.a(project.getName()).newline();
      }
      emitLog(message.toString(), Severity.INFO, logEmitter, span);
    }
  }

  static void sessionEnded(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder message = buffer();
    if (event.getSession().getProjects().size() > 1) {
      logReactorSummary(event.getSession(), message);
    }
    if (event.getSession().getResult().hasExceptions()) {
      message.failure("BUILD FAILURE");
    } else {
      message.success("BUILD SUCCESS");
    }
    logStats(event.getSession(), message);
    message.strong(chars('-', LINE_LENGTH));
    emitLog(message.toString(), Severity.INFO, logEmitter, span);
  }

  static void logReactorSummary(MavenSession session, MessageBuilder message) {
    message.strong(chars('-', LINE_LENGTH)).newline();

    message.strong("Reactor Summary:").newline().newline();

    MavenExecutionResult result = session.getResult();

    for (MavenProject project : session.getProjects()) {
      StringBuilder sb = new StringBuilder(128);

      sb.append(project.getName());
      sb.append(' ');

      if (sb.length() <= MAX_PROJECT_NAME_LENGTH) {
        while (sb.length() < MAX_PROJECT_NAME_LENGTH) {
          sb.append('.');
        }
        sb.append(' ');
      }

      BuildSummary buildSummary = result.getBuildSummary(project);

      if (buildSummary == null) {
        sb.append(buffer().warning("SKIPPED"));
      } else if (buildSummary instanceof BuildSuccess) {
        sb.append(buffer().success("SUCCESS"));
        sb.append(" [");
        String buildTimeDuration = formatDuration(buildSummary.getTime());
        int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
        if (padSize > 0) {
          sb.append(chars(' ', padSize));
        }
        sb.append(buildTimeDuration);
        sb.append(']');
      } else if (buildSummary instanceof BuildFailure) {
        sb.append(buffer().failure("FAILURE"));
        sb.append(" [");
        String buildTimeDuration = formatDuration(buildSummary.getTime());
        int padSize = MAX_PADDED_BUILD_TIME_DURATION_LENGTH - buildTimeDuration.length();
        if (padSize > 0) {
          sb.append(chars(' ', padSize));
        }
        sb.append(buildTimeDuration);
        sb.append(']');
      }

      message.a(sb.toString()).newline();
    }
  }

  @SuppressWarnings("JavaUtilDate")
  static void logStats(MavenSession session, MessageBuilder messageBuilder) {
    messageBuilder.strong(chars('-', LINE_LENGTH)).newline();

    long start = session.getRequest().getStartTime().getTime();
    long finish = System.currentTimeMillis();
    long duration = finish - start;

    String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";

    messageBuilder.a("Total time: " + formatDuration(duration) + wallClock).newline();

    System.gc();

    Runtime r = Runtime.getRuntime();

    long mb = 1024 * 1024;

    messageBuilder
        .a(
            "Final Memory: "
                + (r.totalMemory() - r.freeMemory()) / mb
                + "M/"
                + r.totalMemory() / mb
                + "M")
        .newline();
  }

  static void projectSkipped(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder message = buffer().strong(chars('-', LINE_LENGTH)).newline();
    message.strong("Skipping " + event.getProject().getName()).newline();
    message.a("This project has been banned from the build due to previous failures.").newline();
    message.strong(chars('-', LINE_LENGTH));
    emitLog(message.toString(), Severity.INFO, logEmitter, span);
  }

  static void projectStarted(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder message = buffer().strong(chars('-', LINE_LENGTH)).newline();
    message
        .strong("Building " + event.getProject().getName() + " " + event.getProject().getVersion())
        .newline();
    message.strong(chars('-', LINE_LENGTH));

    emitLog(message.toString(), Severity.INFO, logEmitter, span);
  }

  static void mojoSkipped(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    emitLog(
        "Goal "
            + event.getMojoExecution().getGoal()
            + " requires online mode for execution but Maven is currently offline, skipping",
        Severity.WARN,
        logEmitter,
        span);
  }

  /**
   *
   *
   * <pre>--- mojo-artifactId:version:goal (mojo-executionId) @ project-artifactId ---</pre>
   */
  static void mojoStarted(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder buffer = buffer().strong("--- ");
    appendMojoExecution(buffer, event.getMojoExecution());
    appendMavenProject(buffer, event.getProject());
    buffer.strong(" ---");
    emitLog(buffer.toString(), Severity.INFO, logEmitter, span);
  }

  private static void appendMojoExecution(MessageBuilder message, MojoExecution mojoExecution) {
    message.mojo(
        mojoExecution.getArtifactId()
            + ':'
            + mojoExecution.getVersion()
            + ':'
            + mojoExecution.getGoal());
    if (mojoExecution.getExecutionId() != null) {
      message.a(' ').strong('(' + mojoExecution.getExecutionId() + ')');
    }
  }

  /**
   *
   *
   * <pre>
   * &gt;&gt;&gt; mojo-artifactId:version:goal (mojo-executionId) &gt; :forked-goal @ project-artifactId &gt;&gt;&gt;
   * </pre>
   *
   * <pre>
   * &gt;&gt;&gt; mojo-artifactId:version:goal (mojo-executionId) &gt; [lifecycle]phase @ project-artifactId &gt;&gt;&gt;
   * </pre>
   */
  static void forkStarted(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder buffer = buffer().strong(">>> ");
    appendMojoExecution(buffer, event.getMojoExecution());
    buffer.strong(" > ");
    appendForkInfo(buffer, event.getMojoExecution().getMojoDescriptor());
    appendMavenProject(buffer, event.getProject());
    buffer.strong(" >>>");
    emitLog(buffer.toString(), Severity.INFO, logEmitter, span);
  }

  /**
   *
   *
   * <pre>
   * &lt;&lt;&lt; mojo-artifactId:version:goal (mojo-executionId) &lt; :forked-goal @ project-artifactId &lt;&lt;&lt;
   * </pre>
   *
   * <pre>
   * &lt;&lt;&lt; mojo-artifactId:version:goal (mojo-executionId) &lt; [lifecycle]phase @ project-artifactId &lt;&lt;&lt;
   * </pre>
   */
  static void forkSucceeded(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    MessageBuilder message = buffer().strong("<<< ");
    appendMojoExecution(message, event.getMojoExecution());
    message.strong(" < ");
    appendForkInfo(message, event.getMojoExecution().getMojoDescriptor());
    appendMavenProject(message, event.getProject());
    message.strong(" <<<");
    emitLog(message.toString(), Severity.INFO, logEmitter, span);
  }

  private static void appendForkInfo(MessageBuilder message, MojoDescriptor md) {
    StringBuilder buff = new StringBuilder();
    if (StringUtils.isNotEmpty(md.getExecutePhase())) {
      // forked phase
      if (StringUtils.isNotEmpty(md.getExecuteLifecycle())) {
        buff.append('[');
        buff.append(md.getExecuteLifecycle());
        buff.append(']');
      }
      buff.append(md.getExecutePhase());
    } else {
      // forked goal
      buff.append(':');
      buff.append(md.getExecuteGoal());
    }
    message.strong(buff.toString());
  }

  private static void appendMavenProject(MessageBuilder message, MavenProject project) {
    message.a(" @ ").project(project.getArtifactId());
  }

  static void forkedProjectStarted(ExecutionEvent event, LogEmitter logEmitter, Span span) {
    if (event.getMojoExecution().getForkedExecutions().size() > 1) {
      MessageBuilder message = buffer();
      message
          .strong(chars('>', LINE_LENGTH))
          .strong("Forking " + event.getProject().getName() + " " + event.getProject().getVersion())
          .strong(chars('>', LINE_LENGTH));

      emitLog(message.toString(), Severity.INFO, logEmitter, span);
    }
  }

  private static void emitLog(String msg, Severity se, LogEmitter logEmitter, Span span) {
    logEmitter
        .logBuilder()
        .setBody(msg)
        .setSeverity(se)
        .setContext(Context.current().with(span))
        .emit();
  }

  static String formatDuration(long millis) {
    Duration duration = Duration.ofMillis(millis);
    long seconds = duration.getSeconds();
    return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }

  /** See org.apache.maven.shared.utils.logging.MessageUtils#buffer() */
  private static MessageBuilder buffer() {
    return new PlainMessageBuilder();
  }
}
