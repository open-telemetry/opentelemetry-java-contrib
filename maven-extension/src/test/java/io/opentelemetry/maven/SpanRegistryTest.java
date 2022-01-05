/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;

public class SpanRegistryTest {

  /** MVND reuses the same Maven process and thus the Span Registry is reused. */
  @Test
  public void testSpanRegistryReuseWhenUsingMvnDaemon() {
    SpanRegistry spanRegistry = new SpanRegistry();

    Tracer tracer = OpenTelemetry.noop().getTracer("test");
    Span firstRootSpan = tracer.spanBuilder("com.example:my-jar-1.1.0-SNAPSHOT").startSpan();
    spanRegistry.setRootSpan(firstRootSpan);
    Span firstRemovedRootSpan = spanRegistry.removeRootSpan();
    assertThat(firstRemovedRootSpan).isEqualTo(firstRootSpan);

    Span secondRootSpan = tracer.spanBuilder("com.example:my-jar-2.1.0-SNAPSHOT").startSpan();
    spanRegistry.setRootSpan(secondRootSpan);
    Span secondRemovedSpan = spanRegistry.removeRootSpan();
    assertThat(secondRemovedSpan).isEqualTo(secondRootSpan);
  }
}
