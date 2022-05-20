/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.function.Consumer;
import javax.annotation.Nullable;

final class MicrometerDoubleCounter extends AbstractCounter implements DoubleCounter {

  private MicrometerDoubleCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(double value) {
    if (value >= 0.0) {
      counter(Attributes.empty()).increment(value);
    }
  }

  @Override
  public void add(double value, Attributes attributes) {
    if (value >= 0.0) {
      counter(attributes).increment(value);
    }
  }

  @Override
  public void add(double value, Attributes attributes, Context context) {
    if (value >= 0.0) {
      counter(attributes).increment(value);
    }
  }

  public static DoubleCounterBuilder builder(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    return new Builder(meterSharedState, name, description, unit);
  }

  private static class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleCounterBuilder {
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
    public MicrometerDoubleCounter build() {
      return new MicrometerDoubleCounter(createInstrumentState());
    }

    @Override
    public ObservableDoubleCounter buildWithCallback(
        Consumer<ObservableDoubleMeasurement> callback) {
      MicrometerDoubleCounter instrument = build();
      return instrument.registerDoubleCallback(
          callback,
          new ObservableDoubleMeasurement() {
            @Override
            public void record(double value) {
              record(value, Attributes.empty());
            }

            @Override
            public void record(double value, Attributes attributes) {
              instrument.record(value, attributes);
            }
          });
    }
  }
}
