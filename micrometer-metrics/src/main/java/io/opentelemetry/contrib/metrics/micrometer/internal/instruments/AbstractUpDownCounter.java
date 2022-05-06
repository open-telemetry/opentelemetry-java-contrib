/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.Gauge;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

abstract class AbstractUpDownCounter extends AbstractInstrument {
  private final Map<Attributes, Counter> map = new ConcurrentHashMap<>();

  protected AbstractUpDownCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  protected final void record(Attributes attributes, double value) {
    map.computeIfAbsent(attributes, this::createCounter).incrementDouble(value);
  }

  private Counter createCounter(Attributes attributes) {
    Counter counter = new Counter();
    Gauge.builder(name(), counter, Counter::doubleValue)
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .register(meterRegistry());
    return counter;
  }

  private static final class Counter extends AtomicLong {
    private static final long serialVersionUID = 1927816293512124184L;

    public void incrementDouble(double value) {
      long currentLong;
      long proposedLong;
      do {
        currentLong = get();
        double currentDouble = Double.longBitsToDouble(currentLong);
        double proposedDouble = currentDouble + value;
        proposedLong = Double.doubleToRawLongBits(proposedDouble);
      } while (!compareAndSet(currentLong, proposedLong));
    }
  }
}
