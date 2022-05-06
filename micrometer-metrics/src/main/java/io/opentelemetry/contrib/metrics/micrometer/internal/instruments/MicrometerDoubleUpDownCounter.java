/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.DoubleUpDownCounterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.function.Consumer;
import javax.annotation.Nullable;

final class MicrometerDoubleUpDownCounter extends AbstractUpDownCounter
    implements DoubleUpDownCounter {
  public MicrometerDoubleUpDownCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(double value) {
    record(Attributes.empty(), value);
  }

  @Override
  public void add(double value, Attributes attributes) {
    record(attributes, value);
  }

  @Override
  public void add(double value, Attributes attributes, Context context) {
    record(attributes, value);
  }

  static DoubleUpDownCounterBuilder builder(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    return new Builder(meterSharedState, name, description, unit);
  }

  private static class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleUpDownCounterBuilder {
    public Builder(
        MeterSharedState meterSharedState,
        String name,
        @Nullable String description,
        @Nullable String unit) {
      super(meterSharedState, name, description, unit);
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public MicrometerDoubleUpDownCounter build() {
      return new MicrometerDoubleUpDownCounter(createInstrumentState());
    }

    @Override
    public ObservableDoubleUpDownCounter buildWithCallback(
        Consumer<ObservableDoubleMeasurement> callback) {
      MicrometerDoubleUpDownCounter instrument = build();
      return instrument.registerDoubleCallback(
          callback,
          new ObservableDoubleMeasurement() {
            @Override
            public void record(double value) {
              instrument.record(Attributes.empty(), value);
            }

            @Override
            public void record(double value, Attributes attributes) {
              instrument.record(attributes, value);
            }
          });
    }
  }
}
