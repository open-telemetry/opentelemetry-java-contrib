/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.semconv.SemconvStability;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.Collections;
import java.util.List;
import org.apache.maven.execution.ExecutionEvent;

/** See <a href="https://github.com/snyk/snyk-maven-plugin">Snyk Maven Plugin</a> */
final class SnykMonitorHandler implements MojoGoalExecutionHandler {

  /**
   * Snyk command "reversed engineered" invoking the Snyk CLI on a Maven project with the `-d` debug
   * flag `snyk -d monitor`. See <a href="https://snyk.io/blog/snyk-cli-cheat-sheet/">Snyk CLI Cheat
   * Sheet</a>
   */
  @SuppressWarnings("deprecation") // until old http semconv are dropped
  @Override
  public void enrichSpan(SpanBuilder spanBuilder, ExecutionEvent executionEvent) {
    spanBuilder.setSpanKind(SpanKind.CLIENT);
    spanBuilder.setAttribute(SemanticAttributes.PEER_SERVICE, "snyk.io");
    spanBuilder.setAttribute(SemanticAttributes.RPC_METHOD, "monitor");

    if (SemconvStability.emitStableHttpSemconv()) {
      spanBuilder.setAttribute(SemanticAttributes.URL_FULL, "https://snyk.io/api/v1/monitor/maven");
      spanBuilder.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, "POST");
    }

    if (SemconvStability.emitOldHttpSemconv()) {
      spanBuilder.setAttribute(SemanticAttributes.HTTP_URL, "https://snyk.io/api/v1/monitor/maven");
      spanBuilder.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
    }
  }

  @Override
  public List<MavenGoal> getSupportedGoals() {
    return Collections.singletonList(MavenGoal.create("io.snyk", "snyk-maven-plugin", "monitor"));
  }
}
