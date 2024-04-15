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
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleUpDownCounterBuilder;
import java.util.function.Consumer;

final class MicrometerDoubleUpDownCounter extends AbstractUpDownCounter
    implements DoubleUpDownCounter, ObservableDoubleMeasurement {
  private MicrometerDoubleUpDownCounter(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void add(double value) {
    add(Attributes.empty(), value);
  }

  @Override
  public void add(double value, Attributes attributes) {
    add(attributes, value);
  }

  @Override
  public void add(double value, Attributes attributes, Context context) {
    add(attributes, value);
  }

  @Override
  public void record(double value) {
    record(Attributes.empty(), value);
  }

  @Override
  public void record(double value, Attributes attributes) {
    record(attributes, value);
  }

  static DoubleUpDownCounterBuilder builder(MicrometerLongUpDownCounter.Builder parent) {
    return new Builder(parent);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleUpDownCounterBuilder, ExtendedDoubleUpDownCounterBuilder {
    private Builder(MicrometerLongUpDownCounter.Builder parent) {
      super(parent);
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
      return instrument.registerDoubleCallback(callback, instrument);
    }
  }
}
