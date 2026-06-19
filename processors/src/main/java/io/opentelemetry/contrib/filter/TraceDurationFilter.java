/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.Collection;

/**
 * A {@link TraceFilter} that matches when the wall-clock duration (max end - min start across all
 * spans in the batch sharing a trace ID) exceeds a configurable threshold.
 */
public final class TraceDurationFilter implements TraceFilter {

  private final long thresholdNanos;

  /**
   * Creates a new {@code TraceDurationFilter}.
   *
   * @param threshold the trace duration threshold; traces with wall-clock duration strictly greater
   *     than this are considered interesting
   */
  public TraceDurationFilter(Duration threshold) {
    if (threshold.isNegative()) {
      throw new IllegalArgumentException("threshold must be non-negative, got: " + threshold);
    }
    this.thresholdNanos = threshold.toNanos();
  }

  @Override
  public boolean shouldKeep(String traceId, Collection<SpanData> spans) {
    if (spans.isEmpty()) {
      return false;
    }
    long minStart = Long.MAX_VALUE;
    long maxEnd = Long.MIN_VALUE;
    for (SpanData span : spans) {
      if (span.getStartEpochNanos() < minStart) {
        minStart = span.getStartEpochNanos();
      }
      if (span.getEndEpochNanos() > maxEnd) {
        maxEnd = span.getEndEpochNanos();
      }
    }
    return (maxEnd - minStart) > thresholdNanos;
  }
}
