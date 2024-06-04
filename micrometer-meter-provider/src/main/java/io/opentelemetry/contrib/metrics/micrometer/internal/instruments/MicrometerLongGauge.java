/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedLongGaugeBuilder;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import java.util.function.Consumer;

public final class MicrometerLongGauge extends AbstractGauge
    implements LongGauge, ObservableLongMeasurement {
  public MicrometerLongGauge(InstrumentState instrumentState) {
    super(instrumentState);
  }

  static LongGaugeBuilder builder(MicrometerDoubleGauge.Builder parent) {
    return new Builder(parent);
  }

  @Override
  public void set(long value) {
    record(Attributes.empty(), (double) value);
  }

  @Override
  public void set(long value, Attributes attributes) {
    record(attributes, (double) value);
  }

  @Override
  public void set(long value, Attributes attributes, Context context) {
    record(attributes, (double) value);
  }

  @Override
  public void record(long value) {
    record(Attributes.empty(), (double) value);
  }

  @Override
  public void record(long value, Attributes attributes) {
    record(attributes, (double) value);
  }

  static final class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongGaugeBuilder, ExtendedLongGaugeBuilder {
    private Builder(MicrometerDoubleGauge.Builder parent) {
      super(parent);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public LongGauge build() {
      return new MicrometerLongGauge(createInstrumentState());
    }

    @Override
    public ObservableLongMeasurement buildObserver() {
      return new MicrometerLongGauge(createInstrumentState());
    }

    @Override
    public ObservableLongGauge buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      MicrometerLongGauge instrument = new MicrometerLongGauge(createInstrumentState());
      return instrument.registerLongCallback(callback, instrument);
    }
  }
}
