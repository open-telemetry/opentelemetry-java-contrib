/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * An implementation of {@link CallbackRegistrar} that uses a {@link ScheduledExecutorService} to
 * execute the registered callbacks on the specified schedule.
 */
public final class ScheduledCallbackRegistrar implements CallbackRegistrar {
  private final ScheduledExecutorService scheduledExecutorService;
  private final long period;
  private final TimeUnit timeUnit;
  private final boolean ownsExecutor;
  private final List<Runnable> callbacks;
  @Nullable private volatile ScheduledFuture<?> scheduledFuture;

  ScheduledCallbackRegistrar(
      ScheduledExecutorService scheduledExecutorService,
      long period,
      TimeUnit timeUnit,
      boolean ownsExecutor) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.period = period;
    this.timeUnit = timeUnit;
    this.ownsExecutor = ownsExecutor;
    this.callbacks = new CopyOnWriteArrayList<>();
  }

  public static ScheduledCallbackRegistrarBuilder builder(
      ScheduledExecutorService scheduledExecutorService) {
    Objects.requireNonNull(scheduledExecutorService, "scheduledExecutorService");
    return new ScheduledCallbackRegistrarBuilder(scheduledExecutorService);
  }

  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    if (callback != null) {
      ensureScheduled();
      callbacks.add(callback);
      return () -> callbacks.remove(callback);
    } else {
      return () -> {};
    }
  }

  private synchronized void ensureScheduled() {
    if (scheduledFuture == null) {
      scheduledFuture =
          scheduledExecutorService.scheduleAtFixedRate(this::poll, period, period, timeUnit);
    }
  }

  private void poll() {
    for (Runnable callback : callbacks) {
      callback.run();
    }
  }

  @Override
  public synchronized void close() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }
    if (ownsExecutor) {
      scheduledExecutorService.shutdown();
    }
  }
}
