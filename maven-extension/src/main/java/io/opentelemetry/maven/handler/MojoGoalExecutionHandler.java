/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.maven.MavenGoal;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;

/** Handler that enriches the span of a mojo goal execution for the goals it supports. */
public interface MojoGoalExecutionHandler {

  default void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {}

  List<MavenGoal> getSupportedGoals();
}
