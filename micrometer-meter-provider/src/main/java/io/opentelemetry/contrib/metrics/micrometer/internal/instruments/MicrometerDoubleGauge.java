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
import java.util.function.Consumer;

public final class MicrometerDoubleGauge extends AbstractGauge {

  private MicrometerDoubleGauge(InstrumentState instrumentState) {
    super(instrumentState);
  }

  public static DoubleGaugeBuilder builder(MeterSharedState meterSharedState, String name) {
    return new DoubleBuilder(meterSharedState, name);
  }

  private static class DoubleBuilder extends AbstractInstrumentBuilder<DoubleBuilder>
      implements DoubleGaugeBuilder {

    private DoubleBuilder(MeterSharedState meterSharedState, String name) {
      super(meterSharedState, name);
    }

    @Override
    public DoubleBuilder self() {
      return this;
    }

    @Override
    public LongGaugeBuilder ofLongs() {
      return MicrometerLongGauge.builder(meterSharedState, name, description, unit);
    }

    @Override
    public ObservableDoubleGauge buildWithCallback(Consumer<ObservableDoubleMeasurement> callback) {
      MicrometerDoubleGauge instrument = new MicrometerDoubleGauge(createInstrumentState());
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
