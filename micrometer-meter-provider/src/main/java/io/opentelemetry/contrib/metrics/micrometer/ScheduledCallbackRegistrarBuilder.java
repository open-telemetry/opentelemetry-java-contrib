/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Builder utility class for creating instances of {@link ScheduledCallbackRegistrar}. */
public final class ScheduledCallbackRegistrarBuilder {
  private final ScheduledExecutorService scheduledExecutorService;
  private long period;
  private TimeUnit timeUnit;
  private boolean shutdownExecutorOnClose;

  ScheduledCallbackRegistrarBuilder(ScheduledExecutorService scheduledExecutorService) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.period = 1L;
    this.timeUnit = TimeUnit.SECONDS;
  }

  /** Sets the period between successive executions of each registered callback */
  @CanIgnoreReturnValue
  public ScheduledCallbackRegistrarBuilder setPeriod(long period, TimeUnit unit) {
    Objects.requireNonNull(unit, "unit");
    this.period = period;
    this.timeUnit = unit;
    return this;
  }

  /** Sets the period between successive executions of each registered callback */
  @CanIgnoreReturnValue
  public ScheduledCallbackRegistrarBuilder setPeriod(Duration period) {
    Objects.requireNonNull(period, "period");
    this.period = period.toMillis();
    this.timeUnit = TimeUnit.MILLISECONDS;
    return this;
  }

  /**
   * Sets that the executor should be {@link ScheduledExecutorService#shutdown() shutdown} when the
   * {@link CallbackRegistrar} is {@link CallbackRegistrar#close() closed}.
   */
  @CanIgnoreReturnValue
  public ScheduledCallbackRegistrarBuilder setShutdownExecutorOnClose(
      boolean shutdownExecutorOnClose) {
    this.shutdownExecutorOnClose = shutdownExecutorOnClose;
    return this;
  }

  /**
   * Constructs a new instance of the {@link CallbackRegistrar} based on the builder's values.
   *
   * @return a new instance.
   */
  public CallbackRegistrar build() {
    return new ScheduledCallbackRegistrar(
        scheduledExecutorService, period, timeUnit, shutdownExecutorOnClose);
  }
}
