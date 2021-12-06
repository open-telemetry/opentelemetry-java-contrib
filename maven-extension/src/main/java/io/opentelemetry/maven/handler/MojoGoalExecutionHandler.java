/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import org.apache.maven.execution.ExecutionEvent;

public interface MojoGoalExecutionHandler extends Comparable<MojoGoalExecutionHandler> {

  default void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {}

  boolean supports(ExecutionEvent executionEvent);

  default int ordinal() {
    return 0;
  }

  @Override
  default int compareTo(MojoGoalExecutionHandler other) {
    return Integer.compare(this.ordinal(), other.ordinal());
  }
}
