/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.Collections;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;

/** See https://github.com/snyk/snyk-maven-plugin */
final class SnykMonitorHandler implements MojoGoalExecutionHandler {

  /**
   * Snyk command "reversed engineered" invoking the Snyk CLI on a Maven project with the `-d` debug
   * flag `snyk -d monitor`. See <a href="https://snyk.io/blog/snyk-cli-cheat-sheet/">Snyk CLI Cheat
   * Sheet</a>
   */
  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);
    spanBuilder.setAttribute(MavenOtelSemanticAttributes.PEER_SERVICE, "snyk.io");
    spanBuilder.setAttribute(UrlAttributes.URL_FULL, "https://snyk.io/api/v1/monitor/maven");
    spanBuilder.setAttribute(MavenOtelSemanticAttributes.RPC_METHOD, "monitor");
    spanBuilder.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST");
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(MavenGoal.create("io.snyk", "snyk-maven-plugin", "monitor"));
  }
}
