/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.maven.MavenGoal;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;

public interface MojoGoalExecutionHandler {

  default void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {}

  List<MavenGoal> getSupportedGoals();
}
