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
import io.opentelemetry.extension.incubator.metrics.ExtendedDoubleCounterBuilder;
import java.util.function.Consumer;

final class MicrometerDoubleCounter extends AbstractCounter
    implements DoubleCounter, ObservableDoubleMeasurement {

  private MicrometerDoubleCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(double value) {
    increment(Attributes.empty(), value);
  }

  @Override
  public void add(double value, Attributes attributes) {
    increment(attributes, value);
  }

  @Override
  public void add(double value, Attributes attributes, Context context) {
    increment(attributes, value);
  }

  @Override
  public void record(double value) {
    setMonotonically(Attributes.empty(), value);
  }

  @Override
  public void record(double value, Attributes attributes) {
    setMonotonically(attributes, value);
  }

  public static DoubleCounterBuilder builder(MicrometerLongCounter.Builder parent) {
    return new Builder(parent);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleCounterBuilder, ExtendedDoubleCounterBuilder {
    private Builder(MicrometerLongCounter.Builder parent) {
      super(parent);
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
      return instrument.registerDoubleCallback(callback, instrument);
    }
  }
}
