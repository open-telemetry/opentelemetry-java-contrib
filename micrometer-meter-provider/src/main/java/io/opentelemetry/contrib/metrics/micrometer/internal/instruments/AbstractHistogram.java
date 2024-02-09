/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.DistributionSummary;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.List;
import javax.annotation.Nullable;

abstract class AbstractHistogram extends AbstractInstrument {
  @Nullable private final List<? extends Number> explicitBucketBoundaries;

  protected AbstractHistogram(InstrumentState instrumentState) {
    super(instrumentState);
    this.explicitBucketBoundaries = instrumentState.explicitBucketBoundaries();
  }

  public DistributionSummary distribution(Attributes attributes) {
    return DistributionSummary.builder(name())
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .sla(sla(explicitBucketBoundaries))
        .register(meterRegistry());
  }

  @Nullable
  private static long[] sla(@Nullable List<? extends Number> explicitBucketBoundaries) {
    if (explicitBucketBoundaries == null) {
      return null;
    }
    long[] sla = new long[explicitBucketBoundaries.size()];
    for (int i = 0; i < explicitBucketBoundaries.size(); i++) {
      sla[i] = explicitBucketBoundaries.get(i).longValue();
    }
    return sla;
  }
}
