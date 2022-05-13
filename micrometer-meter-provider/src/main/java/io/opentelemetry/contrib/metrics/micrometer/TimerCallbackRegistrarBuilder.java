/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.time.Duration;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/** Builder utility class for creating instances of {@link TimerCallbackRegistrar}. */
public final class TimerCallbackRegistrarBuilder {
  private String name;
  private boolean isDaemon;
  private long delay;
  private long period;

  TimerCallbackRegistrarBuilder() {
    this.name = "otel-polling-timer";
    this.isDaemon = true;
    this.delay = 1000L;
    this.period = 1000L;
  }

  /** Sets the name of the timer thread. */
  public TimerCallbackRegistrarBuilder setName(String name) {
    this.name = name;
    return this;
  }

  /** Sets whether the timer thread is a daemon thread. */
  public TimerCallbackRegistrarBuilder setDaemon(boolean isDaemon) {
    this.isDaemon = isDaemon;
    return this;
  }

  /** Sets the initial delay for the first execution each registered callback. */
  public TimerCallbackRegistrarBuilder setDelay(long delay, TimeUnit unit) {
    this.delay = unit.toMillis(delay);
    return this;
  }

  /** Sets the initial delay for the first execution each registered callback. */
  public TimerCallbackRegistrarBuilder setDelay(Duration delay) {
    this.delay = delay.toMillis();
    return this;
  }

  /** Sets the delay between successive executions of each registered callback */
  public TimerCallbackRegistrarBuilder setPeriod(long period, TimeUnit unit) {
    this.period = unit.toMillis(period);
    return this;
  }

  /** Sets the delay between successive executions of each registered callback */
  public TimerCallbackRegistrarBuilder setPeriod(Duration period) {
    this.period = period.toMillis();
    return this;
  }

  /**
   * Constructs a new instance of the {@link CallbackRegistrar} based on the builder's values.
   *
   * @return a new instance.
   */
  public CallbackRegistrar build() {
    Timer timer = new Timer(name, isDaemon);
    return new TimerCallbackRegistrar(timer, delay, period);
  }
}
