/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.contrib.inferredspans.internal.SamplingProfiler;
import io.opentelemetry.contrib.inferredspans.internal.util.DisabledOnOpenJ9;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.WINDOWS)
@DisabledOnOpenJ9
class InferredSpansTest {

  private ProfilerTestSetup setup;

  @BeforeEach
  void setUp() {
    InferredSpans.setInstance(null);
  }

  @AfterEach
  void tearDown() {
    if (setup != null) {
      setup.close();
    }
    InferredSpans.setInstance(null);
  }

  @Test
  void testIsEnabled() {
    assertThat(InferredSpans.isEnabled()).isFalse();

    setup = ProfilerTestSetup.create(c -> {});

    assertThat(InferredSpans.isEnabled()).isTrue();

    setup.close();
    setup = null;

    // In a real-world scenario, the close() method would lead to the processor being garbage
    // collected, but to make it deterministic, we manually set the instance to null
    InferredSpans.setInstance(null);
    assertThat(InferredSpans.isEnabled()).isFalse();
  }

  @Test
  void testSetProfilerIntervalWhenDisabled() {
    InferredSpans.setProfilerInterval(Duration.ofMillis(10));

    setup =
        ProfilerTestSetup.create(
            c ->
                c.profilerInterval(Duration.ofSeconds(10))
                    .profilingDuration(Duration.ofMillis(500)));

    // assert that the interval set before the profiler was initialized is ignored
    assertThat(setup.profiler.getConfig().getProfilingInterval())
        .isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testSetProfilerInterval() {
    setup =
        ProfilerTestSetup.create(
            c ->
                c.profilerInterval(Duration.ofSeconds(10))
                    .profilingDuration(Duration.ofMillis(500)));

    SamplingProfiler profiler = setup.profiler;
    await()
        .untilAsserted(
            () -> assertThat(profiler.getProfilingSessions()).isGreaterThanOrEqualTo(1));

    InferredSpans.setProfilerInterval(Duration.ofMillis(100));

    await()
        .timeout(Duration.ofSeconds(2))
        .untilAsserted(
            () -> assertThat(profiler.getProfilingSessions()).isGreaterThanOrEqualTo(2));
  }
}
