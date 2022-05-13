/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.Counter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;

abstract class AbstractCounter extends AbstractInstrument {
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
}
