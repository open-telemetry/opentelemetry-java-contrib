/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;

/**
 * A {@link SpanFilter} that matches spans whose duration exceeds a configurable threshold, causing
 * all batch-colocated spans sharing the same trace ID to be exported.
 */
public final class DurationSpanFilter implements SpanFilter {

  private final long thresholdNanos;

  /**
   * Creates a new {@code DurationSpanFilter}.
   *
   * @param threshold the duration threshold; spans with duration strictly greater than this are
   *     considered interesting
   */
  public DurationSpanFilter(Duration threshold) {
    if (threshold.isNegative()) {
      throw new IllegalArgumentException("threshold must be non-negative, got: " + threshold);
    }
    this.thresholdNanos = threshold.toNanos();
  }

  @Override
  public boolean shouldKeep(SpanData spanData) {
    long durationNanos = spanData.getEndEpochNanos() - spanData.getStartEpochNanos();
    return durationNanos > thresholdNanos;
  }
}
