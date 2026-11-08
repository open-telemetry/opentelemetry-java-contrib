/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a collector exactly once and counts down the shared latch exactly once, whether the
 * collector ran to completion or was cancelled before it started. A future being cancelled does not
 * mean its callable has not been entered, so the pending-to-running and pending-to-cancelled
 * transitions are decided atomically here.
 */
final class CollectorTask implements Callable<Void> {

  private static final int PENDING = 0;
  private static final int RUNNING = 1;
  private static final int CANCELLED = 2;

  private final AtomicInteger state = new AtomicInteger(PENDING);
  private final Runnable collector;
  private final CountDownLatch finished;

  CollectorTask(Runnable collector, CountDownLatch finished) {
    this.collector = collector;
    this.finished = finished;
  }

  @Override
  public Void call() {
    if (!state.compareAndSet(PENDING, RUNNING)) {
      return null;
    }
    try {
      collector.run();
    } finally {
      finished.countDown();
    }
    return null;
  }

  /** Prevents the task from ever running, unless it already started. */
  void cancelIfNotStarted() {
    if (state.compareAndSet(PENDING, CANCELLED)) {
      finished.countDown();
    }
  }
}
