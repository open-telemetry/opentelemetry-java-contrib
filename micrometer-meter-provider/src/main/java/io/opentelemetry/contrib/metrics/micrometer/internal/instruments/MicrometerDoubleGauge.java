/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import io.opentelemetry.extension.incubator.metrics.DoubleGauge;
import io.opentelemetry.extension.incubator.metrics.ExtendedDoubleGaugeBuilder;
import java.util.function.Consumer;

public final class MicrometerDoubleGauge extends AbstractGauge
    implements DoubleGauge, ObservableDoubleMeasurement {

  private MicrometerDoubleGauge(InstrumentState instrumentState) {
    super(instrumentState);
  }

  @Override
  public void set(double value) {
    record(Attributes.empty(), value);
  }

  @Override
  public void set(double value, Attributes attributes) {
    record(attributes, value);
  }

  @Override
  public void record(double value) {
    record(Attributes.empty(), value);
  }

  @Override
  public void record(double value, Attributes attributes) {
    record(attributes, value);
  }

  public static DoubleGaugeBuilder builder(MeterSharedState meterSharedState, String name) {
    return new Builder(meterSharedState, name);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements DoubleGaugeBuilder, ExtendedDoubleGaugeBuilder {

    private Builder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public LongGaugeBuilder ofLongs() {
      return MicrometerLongGauge.builder(this);
    }

    @Override
    public DoubleGauge build() {
      return new MicrometerDoubleGauge(createInstrumentState());
    }

    @Override
    public ObservableDoubleMeasurement buildObserver() {
      return new MicrometerDoubleGauge(createInstrumentState());
    }

    @Override
    public ObservableDoubleGauge buildWithCallback(Consumer<ObservableDoubleMeasurement> callback) {
      MicrometerDoubleGauge instrument = new MicrometerDoubleGauge(createInstrumentState());
      return instrument.registerDoubleCallback(callback, instrument);
    }
  }
}
