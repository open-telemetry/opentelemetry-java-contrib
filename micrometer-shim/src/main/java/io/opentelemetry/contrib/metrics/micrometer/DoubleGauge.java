/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class DoubleGauge {

  static DoubleGaugeBuilder newBuilder(SharedMeterState state, String name) {
    return newBuilder(state, supplier -> Gauge.builder(name, supplier));
  }

  static DoubleGaugeBuilder newBuilder(
      SharedMeterState state, Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory) {
    return new DoubleGaugeBuilder(state, factory);
  }

  static final class DoubleGaugeBuilder implements io.opentelemetry.api.metrics.DoubleGaugeBuilder {
    private final SharedMeterState state;
    private final Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory;

    DoubleGaugeBuilder(
        SharedMeterState state,
        Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory) {
      this.state = state;
      this.factory = factory;
    }

    @Override
    public DoubleGaugeBuilder setDescription(String description) {
      return new DoubleGaugeBuilder(
          state, factory.andThen(builder -> builder.description(description)));
    }

    @Override
    public DoubleGaugeBuilder setUnit(String unit) {
      return new DoubleGaugeBuilder(state, factory.andThen(builder -> builder.baseUnit(unit)));
    }

    @Override
    public LongGaugeBuilder ofLongs() {
      return LongGauge.newBuilder(state, factory);
    }

    @Override
    public void buildWithCallback(Consumer<ObservableDoubleMeasurement> callback) {
      ObservableGaugeMeasurement measurement = new ObservableGaugeMeasurement(state, factory);
      state.registerCallback(() -> callback.accept(measurement));
    }
  }
}
