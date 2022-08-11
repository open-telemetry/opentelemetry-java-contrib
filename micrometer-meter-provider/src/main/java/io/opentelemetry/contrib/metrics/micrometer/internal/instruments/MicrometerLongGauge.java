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
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public final class MicrometerLongGauge extends AbstractGauge {
  public MicrometerLongGauge(InstrumentState instrumentState) {
    super(instrumentState);
  }

  public static LongGaugeBuilder builder(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    return new Builder(meterSharedState, name, description, unit);
  }

  private static class Builder extends AbstractInstrumentBuilder<Builder>
      implements LongGaugeBuilder {
    private Builder(
        MeterSharedState meterSharedState,
        String name,
        @Nullable String description,
        @Nullable String unit) {
      super(meterSharedState, name, description, unit);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public ObservableLongGauge buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      MicrometerLongGauge instrument = new MicrometerLongGauge(createInstrumentState());
      return instrument.registerLongCallback(
          callback,
          new ObservableLongMeasurement() {
            @Override
            public void record(long value) {
              record(value, Attributes.empty());
            }

            @Override
            public void record(long value, Attributes attributes) {
              instrument.record((double) value, attributes);
            }
          });
    }
  }
}
