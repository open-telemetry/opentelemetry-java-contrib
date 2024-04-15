/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.List;

public final class MicrometerDoubleHistogram extends AbstractHistogram implements DoubleHistogram {

  private MicrometerDoubleHistogram(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void record(double value) {
    distribution(Attributes.empty()).record(value);
  }

  @Override
  public void record(double value, Attributes attributes) {
    distribution(attributes).record(value);
  }

  @Override
  public void record(double value, Attributes attributes, Context context) {
    distribution(attributes).record(value);
  }

  public static DoubleHistogramBuilder builder(MeterSharedState meterSharedState, String name) {
    return new Builder(meterSharedState, name);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleHistogramBuilder, ExtendedDoubleHistogramBuilder {
    private Builder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    public DoubleHistogramBuilder setExplicitBucketBoundariesAdvice(List<Double> bucketBoundaries) {
      return super.setExplicitBucketBoundaries(bucketBoundaries);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public LongHistogramBuilder ofLongs() {
      return MicrometerLongHistogram.builder(this);
    }

    @Override
    public DoubleHistogram build() {
      return new MicrometerDoubleHistogram(createInstrumentState());
    }
  }
}
