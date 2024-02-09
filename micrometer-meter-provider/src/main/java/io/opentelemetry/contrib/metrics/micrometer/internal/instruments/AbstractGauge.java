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

abstract class AbstractGauge extends AbstractInstrument {
  private final Map<Attributes, AtomicDoubleCounter> gaugeMap = new ConcurrentHashMap<>();

  protected AbstractGauge(InstrumentState instrumentState) {
    super(instrumentState);
  }

  protected final void recordImpl(double value, Attributes attributes) {
    gaugeMap.computeIfAbsent(attributesOrEmpty(attributes), this::createAsyncGauge).set(value);
  }

  private AtomicDoubleCounter createAsyncGauge(Attributes attributes) {
    AtomicDoubleCounter counter = new AtomicDoubleCounter();
    Gauge.builder(name(), counter, AtomicDoubleCounter::current)
        .description(description())
        .baseUnit(unit())
        .tags(attributesToTags(attributes))
        .register(meterRegistry());
    return counter;
  }
}
