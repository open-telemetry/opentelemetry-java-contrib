/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import java.time.Duration;
import javax.annotation.concurrent.GuardedBy;

public class ExponentialBackoffPeriodicDelay implements PeriodicDelay {
  private final Duration initialDelay;
  private final Object delayNanosLock = new Object();

  @GuardedBy("delayNanosLock")
  private long delayNanos;

  public ExponentialBackoffPeriodicDelay(Duration initialDelay) {
    this.initialDelay = initialDelay;
    delayNanos = initialDelay.toNanos();
  }

  @Override
  public Duration getNextDelay() {
    synchronized (delayNanosLock) {
      long previousValue = delayNanos;
      delayNanos = delayNanos * 2;
      return Duration.ofNanos(previousValue);
    }
  }

  @Override
  public void reset() {
    synchronized (delayNanosLock) {
      delayNanos = initialDelay.toNanos();
    }
  }
}
