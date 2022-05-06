/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.function.Consumer;

public final class MicrometerLongCounter extends AbstractCounter implements LongCounter {

  private MicrometerLongCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(long value) {
    counter(Attributes.empty()).increment((double) value);
  }

  @Override
  public void add(long value, Attributes attributes) {
    counter(attributes).increment((double) value);
  }

  @Override
  public void add(long value, Attributes attributes, Context context) {
    counter(attributes).increment((double) value);
  }

  public static LongCounterBuilder builder(MeterSharedState meterSharedState, String name) {
    return new Builder(meterSharedState, name);
  }

  private static class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongCounterBuilder {
    public Builder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public DoubleCounterBuilder ofDoubles() {
      return MicrometerDoubleCounter.builder(meterSharedState, name, description, unit);
    }

    @Override
    public MicrometerLongCounter build() {
      return new MicrometerLongCounter(createInstrumentState());
    }

    @Override
    public ObservableLongCounter buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      MicrometerLongCounter instrument = build();
      return instrument.registerLongCallback(
          callback,
          new ObservableLongMeasurement() {
            @Override
            public void record(long value) {
              instrument.counter(Attributes.empty()).increment((double) value);
            }

            @Override
            public void record(long value, Attributes attributes) {
              instrument.counter(attributes).increment((double) value);
            }
          });
    }
  }
}
