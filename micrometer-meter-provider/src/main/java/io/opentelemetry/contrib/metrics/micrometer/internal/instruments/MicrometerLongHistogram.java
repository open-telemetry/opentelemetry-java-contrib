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
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import javax.annotation.Nullable;

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

  public static LongHistogramBuilder builder(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    return new Builder(meterSharedState, name, description, unit);
  }

  private static class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongHistogramBuilder {
    private Builder(
        MeterSharedState meterSharedState,
        String name,
        @Nullable String description,
        @Nullable String unit) {
      super(meterSharedState, name, description, unit);
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
