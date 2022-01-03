/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collections;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;

/** See https://github.com/snyk/snyk-maven-plugin */
final class SnykTestHandler implements MojoGoalExecutionHandler {

  /**
   * Snyk command "reversed engineered" invoking the Snyk CLI on a Maven project with the `-d` debug
   * flag `snyk -d test`. See <a href="https://snyk.io/blog/snyk-cli-cheat-sheet/">Snyk CLI Cheat
   * Sheet</a>
   */
  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);
    spanBuilder.setAttribute(SemanticAttributes.PEER_SERVICE, "snyk.io");
    spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, "https://snyk.io/api/v1/test-dep-graph");
    spanBuilder.setAttribute(SemanticAttributes.RPC_METHOD, "test");
    spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(MavenGoal.create("io.snyk", "snyk-maven-plugin", "test"));
  }
}
