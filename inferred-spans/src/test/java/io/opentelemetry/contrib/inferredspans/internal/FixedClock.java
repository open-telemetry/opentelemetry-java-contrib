/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

public class FixedClock extends SpanAnchoredClock {

  private long nanoTime = -1L;

  @Override
  public void onSpanStart(ReadWriteSpan started, Context parentContext) {}

  @Override
  public long nanoTime() {
    if (nanoTime == -1L) {
      return System.nanoTime();
    }
    return nanoTime;
  }

  @Override
  public long getAnchor(Span parent) {
    return 0;
  }

  @Override
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public long toEpochNanos(long anchor, long recordedNanoTime) {
    return recordedNanoTime;
  }

  public void setNanoTime(long nanoTime) {
    this.nanoTime = nanoTime;
  }
}
