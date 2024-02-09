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
    this.explicitBucketBoundaries = instrumentState.explicitBucketBoundariesAdvice();
  }

  public DistributionSummary distribution(Attributes attributes) {
    return DistributionSummary.builder(name())
        .tags(attributesToTags(attributes))
        .description(description())
        .baseUnit(unit())
        .serviceLevelObjectives(serviceLevelObjectives(explicitBucketBoundaries))
        .register(meterRegistry());
  }

  @Nullable
  private static double[] serviceLevelObjectives(
      @Nullable List<? extends Number> explicitBucketBoundaries) {
    if (explicitBucketBoundaries == null) {
      return null;
    }
    double[] slos = new double[explicitBucketBoundaries.size()];
    for (int i = 0; i < explicitBucketBoundaries.size(); i++) {
      slos[i] = explicitBucketBoundaries.get(i).doubleValue();
    }
    return slos;
  }
}
