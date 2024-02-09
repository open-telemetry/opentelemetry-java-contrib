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

abstract class AbstractUpDownCounter extends AbstractInstrument {
  private final Map<Attributes, AtomicDoubleCounter> counterMap = new ConcurrentHashMap<>();

  protected AbstractUpDownCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  protected final void add(Attributes attributes, double value) {
    counter(attributes).increment(value);
  }

  protected final void record(Attributes attributes, double value) {
    counter(attributes).set(value);
  }

  protected final AtomicDoubleCounter counter(Attributes attributes) {
    return counterMap.computeIfAbsent(effectiveAttributes(attributes), this::createCounter);
  }

  private AtomicDoubleCounter createCounter(Attributes attributes) {
    AtomicDoubleCounter counter = new AtomicDoubleCounter();
    Gauge.builder(name(), counter, AtomicDoubleCounter::current)
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .register(meterRegistry());
    return counter;
  }
}
