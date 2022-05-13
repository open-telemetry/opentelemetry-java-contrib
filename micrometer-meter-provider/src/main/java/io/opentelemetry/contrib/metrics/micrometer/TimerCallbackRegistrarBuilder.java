/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.time.Duration;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public final class TimerCallbackRegistrarBuilder {
  private String name;
  private boolean isDaemon;
  private long delay;
  private long period;

  public TimerCallbackRegistrarBuilder() {
    this.name = "otel-polling-timer";
    this.isDaemon = true;
    this.delay = 1000L;
    this.period = 1000L;
  }

  public TimerCallbackRegistrarBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public TimerCallbackRegistrarBuilder setDaemon(boolean isDaemon) {
    this.isDaemon = isDaemon;
    return this;
  }

  public TimerCallbackRegistrarBuilder setDelay(long delay, TimeUnit unit) {
    this.delay = unit.toMillis(delay);
    return this;
  }

  public TimerCallbackRegistrarBuilder setDelay(Duration delay) {
    this.delay = delay.toMillis();
    return this;
  }

  public TimerCallbackRegistrarBuilder setPeriod(long period, TimeUnit unit) {
    this.period = unit.toMillis(period);
    return this;
  }

  public TimerCallbackRegistrarBuilder setPeriod(Duration period) {
    this.period = period.toMillis();
    return this;
  }

  public CallbackRegistrar build() {
    Timer timer = new Timer(name, isDaemon);
    return new TimerCallbackRegistrar(timer, delay, period);
  }
}
