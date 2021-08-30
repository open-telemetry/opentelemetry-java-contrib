/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class LongGauge {

  static LongGaugeBuilder newBuilder(SharedMeterState state, String name) {
    return newBuilder(state, supplier -> Gauge.builder(name, supplier));
  }

  public static LongGaugeBuilder newBuilder(
      SharedMeterState state, Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory) {
    return new LongGaugeBuilder(state, factory);
  }

  static final class LongGaugeBuilder implements io.opentelemetry.api.metrics.LongGaugeBuilder {
    private final SharedMeterState state;
    private final Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory;

    LongGaugeBuilder(
        SharedMeterState state,
        Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory) {
      this.state = state;
      this.factory = factory;
    }

    @Override
    public LongGaugeBuilder setDescription(String description) {
      return new LongGaugeBuilder(
          state, factory.andThen(builder -> builder.description(description)));
    }

    @Override
    public LongGaugeBuilder setUnit(String unit) {
      return new LongGaugeBuilder(state, factory.andThen(builder -> builder.baseUnit(unit)));
    }

    @Override
    public DoubleGaugeBuilder ofDoubles() {
      return DoubleGauge.newBuilder(state, factory);
    }

    @Override
    public void buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      callback.accept(new ObservableGaugeMeasurement(state, factory));
    }
  }
}
