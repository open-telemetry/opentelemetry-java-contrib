/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A {@link TraceFilter} that keeps traces whose overall wall-clock duration (max end - min start
 * across all spans in the batch sharing a trace ID) exceeds a configurable threshold.
 */
public final class TraceDurationFilter implements TraceFilter {

  private final long thresholdNanos;

  /**
   * Creates a new {@code TraceDurationFilter}.
   *
   * @param thresholdMs the trace duration threshold in milliseconds; traces with wall-clock
   *     duration strictly greater than this are considered interesting
   */
  public TraceDurationFilter(long thresholdMs) {
    this.thresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMs);
  }

  @Override
  public boolean shouldKeep(String traceId, Collection<SpanData> spans) {
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
