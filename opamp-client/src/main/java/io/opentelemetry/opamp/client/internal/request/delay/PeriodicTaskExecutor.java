/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PeriodicTaskExecutor {
  private final ScheduledExecutorService executorService;
  private final Lock delaySetLock = new ReentrantLock();
  private final AtomicReference<Runnable> periodicTask = new AtomicReference<>();
  private PeriodicDelay periodicDelay;
  @Nullable private ScheduledFuture<?> scheduledFuture;

  public static PeriodicTaskExecutor create(PeriodicDelay initialPeriodicDelay) {
    return new PeriodicTaskExecutor(
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory()),
        initialPeriodicDelay);
  }

  PeriodicTaskExecutor(
      ScheduledExecutorService executorService, PeriodicDelay initialPeriodicDelay) {
    this.executorService = executorService;
    this.periodicDelay = initialPeriodicDelay;
  }

  public void start(@Nonnull Runnable periodicTask) {
    this.periodicTask.set(periodicTask);
    scheduleNext();
  }

  public void executeNow() {
    executorService.execute(periodicTask.get());
  }

  public void setPeriodicDelay(PeriodicDelay periodicDelay) {
    delaySetLock.lock();
    try {
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
      this.periodicDelay = periodicDelay;
      periodicDelay.reset();
      scheduleNext();
    } finally {
      delaySetLock.unlock();
    }
  }

  public void stop() {
    executorService.shutdown();
  }

  private void scheduleNext() {
    delaySetLock.lock();
    try {
      scheduledFuture =
          executorService.schedule(
              new PeriodicRunner(), periodicDelay.getNextDelay().toNanos(), TimeUnit.NANOSECONDS);
    } finally {
      delaySetLock.unlock();
    }
  }

  private class PeriodicRunner implements Runnable {
    @Override
    public void run() {
      Objects.requireNonNull(periodicTask.get()).run();
      scheduleNext();
    }
  }

  private static class DaemonThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(@Nonnull Runnable r) {
      Thread t = delegate.newThread(r);
      try {
        t.setDaemon(true);
      } catch (SecurityException e) {
        // Well, we tried.
      }
      return t;
    }
  }
}
