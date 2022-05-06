/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.DistributionSummary;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;

abstract class AbstractHistogram extends AbstractInstrument {
  protected AbstractHistogram(InstrumentState instrumentState) {
    super(instrumentState);
  }

  public DistributionSummary distribution(Attributes attributes) {
    return DistributionSummary.builder(name())
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .register(meterRegistry());
  }
}
