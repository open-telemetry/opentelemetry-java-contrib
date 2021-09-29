/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disruptor.trace;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Objects;

/**
 * A {@link SpanProcessor} implementation that uses {@code Disruptor} to execute all the hooks on a
 * another thread.
 */
public final class DisruptorSpanProcessor implements SpanProcessor {

  private final DisruptorEventQueue disruptorEventQueue;
  private final boolean startRequired;
  private final boolean endRequired;

  /**
   * Returns a new Builder for {@link DisruptorSpanProcessor}.
   *
   * @param spanProcessor the {@code List<SpanProcessor>} to where the Span's events are pushed.
   * @return a new {@link DisruptorSpanProcessor}.
   * @throws NullPointerException if the {@code spanProcessor} is {@code null}.
   */
  public static DisruptorSpanProcessorBuilder builder(SpanProcessor spanProcessor) {
    return new DisruptorSpanProcessorBuilder(Objects.requireNonNull(spanProcessor));
  }

  // TODO: Add metrics for dropped spans.

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    if (!startRequired) {
      return;
    }
    disruptorEventQueue.enqueueStartEvent(span, parentContext);
  }

  @Override
  public boolean isStartRequired() {
    return startRequired;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    if (!endRequired) {
      return;
    }
    disruptorEventQueue.enqueueEndEvent(span);
  }

  @Override
  public boolean isEndRequired() {
    return endRequired;
  }

  @Override
  public CompletableResultCode shutdown() {
    return disruptorEventQueue.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return disruptorEventQueue.forceFlush();
  }

  DisruptorSpanProcessor(
      DisruptorEventQueue disruptorEventQueue, boolean startRequired, boolean endRequired) {
    this.disruptorEventQueue = disruptorEventQueue;
    this.startRequired = startRequired;
    this.endRequired = endRequired;
  }
}
