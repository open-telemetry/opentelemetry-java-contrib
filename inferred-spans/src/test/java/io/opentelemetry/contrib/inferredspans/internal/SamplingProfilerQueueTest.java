/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.inferredspans.ProfilerTestSetup;
import io.opentelemetry.contrib.inferredspans.internal.util.DisabledOnOpenJ9;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class SamplingProfilerQueueTest {

  @Test
  @DisabledOnOs(OS.WINDOWS)
  @DisabledOnOpenJ9
  void testFillQueue() throws Exception {

    try (ProfilerTestSetup setup =
        ProfilerTestSetup.create(
            config -> config.clock(new FixedClock()).startScheduledProfiling(false))) {

      setup.profiler.setProfilingSessionOngoing(true);

      Span traceContext =
          Span.wrap(
              SpanContext.create(
                  "0af7651916cd43dd8448eb211c80319c",
                  "b7ad6b7169203331",
                  TraceFlags.getSampled(),
                  TraceState.getDefault()));

      assertThat(setup.profiler.onActivation(traceContext, null)).isTrue();

      for (int i = 0; i < SamplingProfiler.RING_BUFFER_SIZE - 1; i++) {
        assertThat(setup.profiler.onActivation(traceContext, null)).isTrue();
      }

      // no more free slots after adding RING_BUFFER_SIZE events
      assertThat(setup.profiler.onActivation(traceContext, null)).isFalse();

      setup.profiler.consumeActivationEventsFromRingBufferAndWriteToFile();

      // now there should be free slots
      assertThat(setup.profiler.onActivation(traceContext, null)).isTrue();
    }
  }
}
