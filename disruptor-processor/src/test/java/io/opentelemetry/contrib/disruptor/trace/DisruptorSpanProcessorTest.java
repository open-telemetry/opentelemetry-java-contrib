/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disruptor.trace;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DisruptorSpanProcessor}. */
@ExtendWith(MockitoExtension.class)
class DisruptorSpanProcessorTest {
  private static final boolean REQUIRED = true;
  private static final boolean NOT_REQUIRED = false;

  @Mock private ReadableSpan readableSpan;
  @Mock private ReadWriteSpan readWriteSpan;

  // EventQueueEntry for incrementing a Counter.
  private static class IncrementSpanProcessor implements SpanProcessor {
    private final AtomicInteger counterOnStart = new AtomicInteger(0);
    private final AtomicInteger counterOnEnd = new AtomicInteger(0);
    private final AtomicInteger counterEndSpans = new AtomicInteger(0);
    private final AtomicInteger counterOnShutdown = new AtomicInteger(0);
    private final AtomicInteger counterOnForceFlush = new AtomicInteger(0);
    private final AtomicInteger counterOnExportedForceFlushSpans = new AtomicInteger(0);
    private final AtomicInteger deltaExportedForceFlushSpans = new AtomicInteger(0);
    private final boolean startRequired;
    private final boolean endRequired;

    private IncrementSpanProcessor(boolean startRequired, boolean endRequired) {
      this.startRequired = startRequired;
      this.endRequired = endRequired;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
      counterOnStart.incrementAndGet();
    }

    @Override
    public boolean isStartRequired() {
      return startRequired;
    }

    @Override
    public void onEnd(ReadableSpan span) {
      counterOnEnd.incrementAndGet();
      counterEndSpans.incrementAndGet();
    }

    @Override
    public boolean isEndRequired() {
      return endRequired;
    }

    @Override
    public CompletableResultCode shutdown() {
      counterOnShutdown.incrementAndGet();
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode forceFlush() {
      counterOnForceFlush.incrementAndGet();
      deltaExportedForceFlushSpans.set(counterEndSpans.getAndSet(0));
      counterOnExportedForceFlushSpans.addAndGet(deltaExportedForceFlushSpans.get());
      return CompletableResultCode.ofSuccess();
    }

    private int getCounterOnStart() {
      return counterOnStart.get();
    }

    private int getCounterOnEnd() {
      return counterOnEnd.get();
    }

    private int getCounterOnShutdown() {
      return counterOnShutdown.get();
    }

    private int getCounterOnForceFlush() {
      return counterOnForceFlush.get();
    }

    public int getCounterOnExportedForceFlushSpans() {
      return counterOnExportedForceFlushSpans.get();
    }

    public int getDeltaExportedForceFlushSpans() {
      return deltaExportedForceFlushSpans.get();
    }
  }

  @Test
  void incrementOnce() {
    IncrementSpanProcessor incrementSpanProcessor = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    assertThat(disruptorSpanProcessor.isStartRequired()).isTrue();
    assertThat(disruptorSpanProcessor.isEndRequired()).isTrue();
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(0);
    disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
    disruptorSpanProcessor.onEnd(readableSpan);
    disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnForceFlush()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void incrementOnce_NoStart() {
    IncrementSpanProcessor incrementSpanProcessor =
        new IncrementSpanProcessor(NOT_REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    assertThat(disruptorSpanProcessor.isStartRequired()).isFalse();
    assertThat(disruptorSpanProcessor.isEndRequired()).isTrue();
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(0);
    disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
    disruptorSpanProcessor.onEnd(readableSpan);
    disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnForceFlush()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void incrementOnce_NoEnd() {
    IncrementSpanProcessor incrementSpanProcessor =
        new IncrementSpanProcessor(REQUIRED, NOT_REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    assertThat(disruptorSpanProcessor.isStartRequired()).isTrue();
    assertThat(disruptorSpanProcessor.isEndRequired()).isFalse();
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(0);
    disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
    disruptorSpanProcessor.onEnd(readableSpan);
    disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnForceFlush()).isEqualTo(1);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void shutdownIsCalledOnlyOnce() {
    IncrementSpanProcessor incrementSpanProcessor = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void incrementAfterShutdown() {
    IncrementSpanProcessor incrementSpanProcessor = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
    disruptorSpanProcessor.onEnd(readableSpan);
    disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(0);
    assertThat(incrementSpanProcessor.getCounterOnForceFlush()).isEqualTo(0);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void incrementTenK() {
    final int tenK = 10000;
    IncrementSpanProcessor incrementSpanProcessor = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    for (int i = 1; i <= tenK; i++) {
      disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
      disruptorSpanProcessor.onEnd(readableSpan);
      if (i % 10 == 0) {
        disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
      }
    }
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(tenK);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(tenK);
    assertThat(incrementSpanProcessor.getCounterOnForceFlush()).isEqualTo(tenK / 10);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }

  @Test
  void incrementMultiSpanProcessor() {
    IncrementSpanProcessor incrementSpanProcessor1 = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    IncrementSpanProcessor incrementSpanProcessor2 = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(
                SpanProcessor.composite(
                    Arrays.asList(incrementSpanProcessor1, incrementSpanProcessor2)))
            .build();
    disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
    disruptorSpanProcessor.onEnd(readableSpan);
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor1.getCounterOnStart()).isEqualTo(1);
    assertThat(incrementSpanProcessor1.getCounterOnEnd()).isEqualTo(1);
    assertThat(incrementSpanProcessor1.getCounterOnShutdown()).isEqualTo(1);
    assertThat(incrementSpanProcessor1.getCounterOnForceFlush()).isEqualTo(0);
    assertThat(incrementSpanProcessor2.getCounterOnStart()).isEqualTo(1);
    assertThat(incrementSpanProcessor2.getCounterOnEnd()).isEqualTo(1);
    assertThat(incrementSpanProcessor2.getCounterOnShutdown()).isEqualTo(1);
    assertThat(incrementSpanProcessor2.getCounterOnForceFlush()).isEqualTo(0);
  }

  @Test
  void multipleForceFlush() {
    final int tenK = 10000;
    IncrementSpanProcessor incrementSpanProcessor = new IncrementSpanProcessor(REQUIRED, REQUIRED);
    DisruptorSpanProcessor disruptorSpanProcessor =
        DisruptorSpanProcessor.builder(incrementSpanProcessor).build();
    for (int i = 1; i <= tenK; i++) {
      disruptorSpanProcessor.onStart(Context.root(), readWriteSpan);
      disruptorSpanProcessor.onEnd(readableSpan);
      if (i % 100 == 0) {
        disruptorSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
        assertThat(incrementSpanProcessor.getDeltaExportedForceFlushSpans()).isEqualTo(100);
      }
    }
    disruptorSpanProcessor.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(incrementSpanProcessor.getCounterOnStart()).isEqualTo(tenK);
    assertThat(incrementSpanProcessor.getCounterOnEnd()).isEqualTo(tenK);
    assertThat(incrementSpanProcessor.getCounterOnExportedForceFlushSpans()).isEqualTo(tenK);
    assertThat(incrementSpanProcessor.getCounterOnShutdown()).isEqualTo(1);
  }
}
