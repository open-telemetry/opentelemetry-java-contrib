/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DurationSpanFilterTest {

  private static final long THRESHOLD_MS = 2000L;
  private static final long THRESHOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(THRESHOLD_MS);

  private final DurationSpanFilter filter = new DurationSpanFilter(THRESHOLD_MS);

  @Test
  void spanOverThresholdIsKept() {
    SpanData span = spanWithDurationNanos(THRESHOLD_NANOS + 1);
    assertThat(filter.shouldKeep(span)).isTrue();
  }

  @Test
  void spanExactlyAtThresholdIsDropped() {
    SpanData span = spanWithDurationNanos(THRESHOLD_NANOS);
    assertThat(filter.shouldKeep(span)).isFalse();
  }

  @Test
  void spanUnderThresholdIsDropped() {
    SpanData span = spanWithDurationNanos(TimeUnit.MILLISECONDS.toNanos(500));
    assertThat(filter.shouldKeep(span)).isFalse();
  }

  private static SpanData spanWithDurationNanos(long durationNanos) {
    SpanData span = mock(SpanData.class);
    long startNanos = TimeUnit.MILLISECONDS.toNanos(1_000_000_000L);
    when(span.getStartEpochNanos()).thenReturn(startNanos);
    when(span.getEndEpochNanos()).thenReturn(startNanos + durationNanos);
    return span;
  }
}
