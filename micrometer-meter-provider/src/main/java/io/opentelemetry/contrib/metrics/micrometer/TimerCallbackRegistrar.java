/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

/**
 * An implementation of {@link CallbackRegistrar} that uses a {@link Timer} to execute the
 * registered callbacks on the specified periodicity.
 */
public final class TimerCallbackRegistrar implements CallbackRegistrar {
  private final Timer timer;
  private final Function<Runnable, TimerTask> timerTaskFunction;
  final long delay;
  final long period;

  TimerCallbackRegistrar(Timer timer, long delay, long period) {
    this(timer, delay, period, RunnableTimerTask::new);
  }

  TimerCallbackRegistrar(
      Timer timer, long delay, long period, Function<Runnable, TimerTask> timerTaskFunction) {
    this.timer = timer;
    this.delay = delay;
    this.period = period;
    this.timerTaskFunction = timerTaskFunction;
  }

  public static TimerCallbackRegistrarBuilder builder() {
    return new TimerCallbackRegistrarBuilder();
  }

  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    if (callback != null) {
      TimerTask task = timerTaskFunction.apply(callback);
      timer.scheduleAtFixedRate(task, delay, period);
      return task::cancel;
    } else {
      return () -> {};
    }
  }

  @Override
  public void close() {
    timer.cancel();
  }

  private static class RunnableTimerTask extends TimerTask {
    private final Runnable runnable;

    public RunnableTimerTask(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      runnable.run();
    }
  }
}
