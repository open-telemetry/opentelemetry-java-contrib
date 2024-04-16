/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedLongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.DoubleUpDownCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.function.Consumer;

public final class MicrometerLongUpDownCounter extends AbstractUpDownCounter
    implements LongUpDownCounter, ObservableLongMeasurement {
  private MicrometerLongUpDownCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(long value) {
    add(Attributes.empty(), (double) value);
  }

  @Override
  public void add(long value, Attributes attributes) {
    add(attributes, (double) value);
  }

  @Override
  public void add(long value, Attributes attributes, Context context) {
    add(attributes, (double) value);
  }

  @Override
  public void record(long value) {
    record(Attributes.empty(), (double) value);
  }

  @Override
  public void record(long value, Attributes attributes) {
    record(attributes, (double) value);
  }

  public static LongUpDownCounterBuilder builder(MeterSharedState meterSharedState, String name) {
    return new Builder(meterSharedState, name);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongUpDownCounterBuilder, ExtendedLongUpDownCounterBuilder {
    private Builder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public DoubleUpDownCounterBuilder ofDoubles() {
      return MicrometerDoubleUpDownCounter.builder(this);
    }

    @Override
    public MicrometerLongUpDownCounter build() {
      return new MicrometerLongUpDownCounter(createInstrumentState());
    }

    @Override
    public ObservableLongUpDownCounter buildWithCallback(
        Consumer<ObservableLongMeasurement> callback) {
      MicrometerLongUpDownCounter instrument = build();
      return instrument.registerLongCallback(callback, instrument);
    }
  }
}
