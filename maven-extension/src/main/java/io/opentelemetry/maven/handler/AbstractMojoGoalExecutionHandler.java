/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Objects;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMojoGoalExecutionHandler implements MojoGoalExecutionHandler {
  private static final Logger logger =
      LoggerFactory.getLogger(AbstractMojoGoalExecutionHandler.class);

  public boolean supports(ExecutionEvent executionEvent) {
    logger.trace("OpenTelemetry: MojoGoalExecutionHandler: supports({}, {})", executionEvent, this);
    if (ExecutionEvent.Type.MojoStarted.equals(executionEvent.getType())) {
      MojoExecution mojoExecution = executionEvent.getMojoExecution();
      for (MavenGoal mavenGoal : getSupportedGoals()) {
        if (Objects.equals(mojoExecution.getGroupId(), mavenGoal.groupId())
            && Objects.equals(mojoExecution.getArtifactId(), mavenGoal.artifactId())
            && Objects.equals(mojoExecution.getGoal(), mavenGoal.goal())) {
          logger.trace(
              "OpenTelemetry: MojoGoalExecutionHandler: supports({}, {}): true",
              executionEvent,
              this);
          return true;
        }
      }
    }
    logger.trace(
        "OpenTelemetry: MojoGoalExecutionHandler: supports({}, {}): false", executionEvent, this);
    return false;
  }

  protected abstract List<MavenGoal> getSupportedGoals();

  @AutoValue
  abstract static class MavenGoal {
    static MavenGoal create(String groupId, String artifactId, String goal) {
      return new AutoValue_AbstractMojoGoalExecutionHandler_MavenGoal(groupId, artifactId, goal);
    }

    abstract String groupId();

    abstract String artifactId();

    abstract String goal();
  }
}
