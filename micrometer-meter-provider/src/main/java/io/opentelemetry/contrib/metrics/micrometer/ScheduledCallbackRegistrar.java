/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link CallbackRegistrar} that uses a {@link ScheduledExecutorService} to
 * execute the registered callbacks on the specified schedule.
 */
public final class ScheduledCallbackRegistrar implements CallbackRegistrar {
  private final ScheduledExecutorService scheduledExecutorService;
  private final long period;
  private final TimeUnit timeUnit;

  ScheduledCallbackRegistrar(
      ScheduledExecutorService scheduledExecutorService, long period, TimeUnit timeUnit) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.period = period;
    this.timeUnit = timeUnit;
  }

  public static ScheduledCallbackRegistrarBuilder builder(
      ScheduledExecutorService scheduledExecutorService) {
    Objects.requireNonNull(scheduledExecutorService, "scheduledExecutorService");
    return new ScheduledCallbackRegistrarBuilder(scheduledExecutorService);
  }

  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    if (callback != null) {
      ScheduledFuture<?> future =
          scheduledExecutorService.scheduleAtFixedRate(callback, period, period, timeUnit);
      return () -> future.cancel(false);
    } else {
      return () -> {};
    }
  }

  @Override
  public void close() {
    scheduledExecutorService.shutdown();
  }
}
