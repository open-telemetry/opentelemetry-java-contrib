/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public final class MicrometerDoubleGauge extends AbstractInstrument {
  private final MultiGauge multiGauge;

  private MicrometerDoubleGauge(InstrumentState instrumentState) {
    super(instrumentState);
    multiGauge =
        MultiGauge.builder(name())
            .description(description())
            .baseUnit(unit())
            .register(meterRegistry());
  }

  @SuppressWarnings("rawtypes")
  private void refreshLongGauges(Consumer<ObservableLongMeasurement> callback) {
    List<MultiGauge.Row> rows = new ArrayList<>();
    callback.accept(
        new ObservableLongMeasurement() {
          @Override
          public void record(long value) {
            record(value, Attributes.empty());
          }

          @Override
          public void record(long value, Attributes attributes) {
            rows.add(MultiGauge.Row.of(Tags.of(attributesToTags(attributes)), value));
          }
        });
    multiGauge.register(rows, /* overwrite= */ true);
  }

  @SuppressWarnings("rawtypes")
  private void refreshDoubleGauges(Consumer<ObservableDoubleMeasurement> callback) {
    List<MultiGauge.Row> rows = new ArrayList<>();
    callback.accept(
        new ObservableDoubleMeasurement() {
          @Override
          public void record(double value) {
            record(value, Attributes.empty());
          }

          @Override
          public void record(double value, Attributes attributes) {
            rows.add(MultiGauge.Row.of(Tags.of(attributesToTags(attributes)), value));
          }
        });
    multiGauge.register(rows, /* overwrite= */ true);
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
      return new LongBuilder(meterSharedState, name, description, unit);
    }

    @Override
    public ObservableDoubleGauge buildWithCallback(Consumer<ObservableDoubleMeasurement> callback) {
      MicrometerDoubleGauge instrument = new MicrometerDoubleGauge(createInstrumentState());
      return instrument.registerCallback(() -> instrument.refreshDoubleGauges(callback));
    }
  }

  private static class LongBuilder extends AbstractInstrumentBuilder<LongBuilder>
      implements LongGaugeBuilder {
    private LongBuilder(
        MeterSharedState meterSharedState,
        String name,
        @Nullable String description,
        @Nullable String unit) {
      super(meterSharedState, name, description, unit);
    }

    @Override
    public LongBuilder self() {
      return this;
    }

    @Override
    public ObservableLongGauge buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      MicrometerDoubleGauge instrument = new MicrometerDoubleGauge(createInstrumentState());
      return instrument.registerCallback(() -> instrument.refreshLongGauges(callback));
    }
  }
}
