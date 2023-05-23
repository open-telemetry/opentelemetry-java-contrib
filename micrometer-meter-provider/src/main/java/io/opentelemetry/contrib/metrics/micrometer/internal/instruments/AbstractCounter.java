/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractCounter extends AbstractInstrument {
  private final Map<Attributes, AtomicDoubleCounter> counterMap = new ConcurrentHashMap<>();

  protected AbstractCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  protected final Counter counter(Attributes attributes) {
    return Counter.builder(name())
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .register(meterRegistry());
  }

  protected final void record(double value, Attributes attributes) {
    counterMap
        .computeIfAbsent(attributesOrEmpty(attributes), this::createAsyncCounter)
        .setMonotonically(value);
  }

  private AtomicDoubleCounter createAsyncCounter(Attributes attributes) {
    AtomicDoubleCounter counter = new AtomicDoubleCounter();
    FunctionCounter.builder(name(), counter, AtomicDoubleCounter::current)
        .description(description())
        .baseUnit(unit())
        .tags(attributesToTags(attributes))
        .register(meterRegistry());
    return counter;
  }
}
