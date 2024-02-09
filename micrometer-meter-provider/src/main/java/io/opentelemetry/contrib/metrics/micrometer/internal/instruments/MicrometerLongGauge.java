/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.extension.incubator.metrics.ExtendedLongGaugeBuilder;
import io.opentelemetry.extension.incubator.metrics.LongGauge;
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
    recordImpl((double) value, Attributes.empty());
  }

  @Override
  public void set(long value, Attributes attributes) {
    recordImpl((double) value, attributes);
  }

  @Override
  public void record(long value) {
    recordImpl((double) value, Attributes.empty());
  }

  @Override
  public void record(long value, Attributes attributes) {
    recordImpl((double) value, attributes);
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
