/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.api.incubator.metrics.ExtendedLongHistogramBuilder;
import java.util.List;

final class MicrometerLongHistogram extends AbstractHistogram implements LongHistogram {

  private MicrometerLongHistogram(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void record(long value) {
    distribution(Attributes.empty()).record((double) value);
  }

  @Override
  public void record(long value, Attributes attributes) {
    distribution(attributes).record((double) value);
  }

  @Override
  public void record(long value, Attributes attributes, Context context) {
    distribution(attributes).record((double) value);
  }

  public static LongHistogramBuilder builder(MicrometerDoubleHistogram.Builder parent) {
    return new Builder(parent);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongHistogramBuilder, ExtendedLongHistogramBuilder {
    private Builder(MicrometerDoubleHistogram.Builder parent) {
      super(parent);
    }

    @Override
    public LongHistogramBuilder setExplicitBucketBoundariesAdvice(List<Long> bucketBoundaries) {
      return super.setExplicitBucketBoundaries(bucketBoundaries);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public LongHistogram build() {
      return new MicrometerLongHistogram(createInstrumentState());
    }
  }
}
