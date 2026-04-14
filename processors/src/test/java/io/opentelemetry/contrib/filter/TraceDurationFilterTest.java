/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TraceDurationFilterTest {

  private static final Duration THRESHOLD = Duration.ofSeconds(10);
  private static final long THRESHOLD_NANOS = THRESHOLD.toNanos();

  private final TraceDurationFilter filter = new TraceDurationFilter(THRESHOLD);

  @Test
  void traceOverThresholdIsKept() {
    SpanData early = spanAt(0, 500);
    SpanData late = spanAt(11500, 500);

    assertThat(filter.shouldKeep("trace-1", Arrays.asList(early, late))).isTrue();
  }

  @Test
  void traceExactlyAtThresholdIsDropped() {
    SpanData span1 = spanAtNanos(0, 100_000_000L);
    SpanData span2 = spanAtNanos(THRESHOLD_NANOS - 100_000_000L, 100_000_000L);

    assertThat(filter.shouldKeep("trace-1", Arrays.asList(span1, span2))).isFalse();
  }

  @Test
  void traceJustOverThresholdIsKept() {
    SpanData span1 = spanAtNanos(0, 100_000_000L);
    SpanData span2 = spanAtNanos(THRESHOLD_NANOS - 100_000_000L + 1, 100_000_000L);

    assertThat(filter.shouldKeep("trace-1", Arrays.asList(span1, span2))).isTrue();
  }

  @Test
  void traceUnderThresholdIsDropped() {
    SpanData span1 = spanAt(0, 500);
    SpanData span2 = spanAt(4000, 500);

    assertThat(filter.shouldKeep("trace-1", Arrays.asList(span1, span2))).isFalse();
  }

  @Test
  void singleSpanTrace() {
    SpanData span = spanAt(0, 500);

    assertThat(filter.shouldKeep("trace-1", Collections.singletonList(span))).isFalse();
  }

  @Test
  void emptySpanListReturnsFalse() {
    assertThat(filter.shouldKeep("trace-1", Collections.emptyList())).isFalse();
  }

  @Test
  void negativeThresholdThrows() {
    assertThatThrownBy(() -> new TraceDurationFilter(Duration.ofMillis(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("threshold must be non-negative");
  }

  private static SpanData spanAt(long startOffsetMs, long durationMs) {
    return spanAtNanos(
        TimeUnit.MILLISECONDS.toNanos(startOffsetMs), TimeUnit.MILLISECONDS.toNanos(durationMs));
  }

  private static SpanData spanAtNanos(long startOffsetNanos, long durationNanos) {
    SpanData span = mock(SpanData.class);
    long baseNanos = TimeUnit.MILLISECONDS.toNanos(1_000_000_000L);
    long startNanos = baseNanos + startOffsetNanos;
    when(span.getStartEpochNanos()).thenReturn(startNanos);
    when(span.getEndEpochNanos()).thenReturn(startNanos + durationNanos);
    return span;
  }
}
