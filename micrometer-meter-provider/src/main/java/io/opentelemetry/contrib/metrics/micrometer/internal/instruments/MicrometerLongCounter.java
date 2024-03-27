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
import io.opentelemetry.extension.incubator.metrics.ExtendedLongCounterBuilder;
import java.util.function.Consumer;

public final class MicrometerLongCounter extends AbstractCounter
    implements LongCounter, ObservableLongMeasurement {

  private MicrometerLongCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(long value) {
    increment(Attributes.empty(), (double) value);
  }

  @Override
  public void add(long value, Attributes attributes) {
    increment(attributes, (double) value);
  }

  @Override
  public void add(long value, Attributes attributes, Context context) {
    increment(attributes, (double) value);
  }

  @Override
  public void record(long value) {
    setMonotonically(Attributes.empty(), (double) value);
  }

  @Override
  public void record(long value, Attributes attributes) {
    setMonotonically(attributes, (double) value);
  }

  public static LongCounterBuilder builder(MeterSharedState meterSharedState, String name) {
    return new Builder(meterSharedState, name);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongCounterBuilder, ExtendedLongCounterBuilder {
    private Builder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public DoubleCounterBuilder ofDoubles() {
      return MicrometerDoubleCounter.builder(this);
    }

    @Override
    public MicrometerLongCounter build() {
      return new MicrometerLongCounter(createInstrumentState());
    }

    @Override
    public ObservableLongCounter buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      MicrometerLongCounter instrument = build();
      return instrument.registerLongCallback(callback, instrument);
    }
  }
}
