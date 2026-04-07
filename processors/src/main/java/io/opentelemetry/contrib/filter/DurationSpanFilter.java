/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SpanFilter} that keeps traces containing any span whose duration exceeds a configurable
 * threshold.
 */
public final class DurationSpanFilter implements SpanFilter {

  private final long thresholdNanos;

  /**
   * Creates a new {@code DurationSpanFilter}.
   *
   * @param thresholdMs the duration threshold in milliseconds; spans with duration strictly greater
   *     than this are considered interesting
   */
  public DurationSpanFilter(long thresholdMs) {
    this.thresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMs);
  }

  @Override
  public boolean shouldKeep(SpanData spanData) {
    long durationNanos = spanData.getEndEpochNanos() - spanData.getStartEpochNanos();
    return durationNanos > thresholdNanos;
  }
}
