/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.profiler;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.otel.testing.DisabledOnOpenJ9;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
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
