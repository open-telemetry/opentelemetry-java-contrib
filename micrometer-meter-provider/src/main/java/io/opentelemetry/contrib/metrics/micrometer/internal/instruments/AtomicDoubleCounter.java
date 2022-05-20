/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

final class AtomicDoubleCounter {
  private static final AtomicLongFieldUpdater<AtomicDoubleCounter> BITS_UPDATER =
      AtomicLongFieldUpdater.newUpdater(AtomicDoubleCounter.class, "doubleBits");

  @SuppressWarnings("UnusedVariable")
  private volatile long doubleBits;

  public double current() {
    return Double.longBitsToDouble(doubleBits);
  }

  public boolean increment(double increment) {
    while (true) {
      double current = current();
      double update = current + increment;
      if (compareAndSet(current, update)) {
        return true;
      }
    }
  }

  public boolean set(double value) {
    BITS_UPDATER.set(this, Double.doubleToRawLongBits(value));
    return true;
  }

  public boolean setMonotonically(double value) {
    while (true) {
      double current = current();
      if (current > value) {
        return false;
      }
      if (compareAndSet(current, value)) {
        return true;
      }
    }
  }

  public boolean compareAndSet(double expected, double update) {
    long expectedBits = Double.doubleToRawLongBits(expected);
    long updateBits = Double.doubleToRawLongBits(update);
    return BITS_UPDATER.compareAndSet(this, expectedBits, updateBits);
  }
}
