/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleCounter;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.context.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class DoubleCounter implements io.opentelemetry.api.metrics.DoubleCounter {
  private final SharedMeterState state;
  private final Reader<Counter.Builder> factory;
  private final Map<Attributes, Counter> map;

  private DoubleCounter(SharedMeterState state, Reader<Counter.Builder> factory) {
    this.state = state;
    this.factory = factory;
    this.map = new ConcurrentHashMap<>();
  }

  @Override
  public void add(double value) {
    add(value, Attributes.empty(), Context.current());
  }

  @Override
  public void add(double value, Attributes attributes) {
    add(value, attributes, Context.current());
  }

  @Override
  public void add(double value, Attributes attributes, Context context) {
    BoundDoubleCounter bound = bind(attributes);
    try {
      bound.add(value, context);
    } finally {
      bound.unbind();
    }
  }

  @Override
  public BoundDoubleCounter bind(Attributes attributes) {
    Counter counter = map.computeIfAbsent(attributes, this::createCounter);
    return new Bound(counter);
  }

  private Counter createCounter(Attributes attributes) {
    Tags tags = TagUtils.attributesToTags(attributes);
    return factory.get().tags(tags).register(state.meterRegistry());
  }

  static DoubleCounterBuilder newBuilder(SharedMeterState state, Reader<Counter.Builder> factory) {
    return new Builder(state, factory);
  }

  static final class Builder implements DoubleCounterBuilder {
    private final SharedMeterState state;
    private final Reader<Counter.Builder> factory;

    Builder(SharedMeterState state, Reader<Counter.Builder> factory) {
      this.state = state;
      this.factory = factory;
    }

    @Override
    public Builder setDescription(String description) {
      return new Builder(state, factory.andThen(builder -> builder.description(description)));
    }

    @Override
    public Builder setUnit(String unit) {
      return new Builder(state, factory.andThen(builder -> builder.baseUnit(unit)));
    }

    @Override
    public LongCounterBuilder ofLongs() {
      return LongCounter.newBuilder(state, factory);
    }

    @Override
    public DoubleCounter build() {
      return new DoubleCounter(state, factory);
    }

    @Override
    public void buildWithCallback(Consumer<ObservableDoubleMeasurement> callback) {
      DoubleCounter counter = build();
      state.registerCallback(() -> callback.accept(
          new ObservableDoubleMeasurement() {
            @Override
            public void observe(double value) {
              counter.add(value);
            }

            @Override
            public void observe(double value, Attributes attributes) {
              counter.add(value, attributes);
            }
          }));
    }
  }

  static final class Bound implements BoundDoubleCounter {
    private final Counter counter;

    Bound(Counter counter) {
      this.counter = counter;
    }

    @Override
    public void add(double value) {
      add(value, Context.current());
    }

    @Override
    public void add(double value, Context context) {
      counter.increment(value);
    }

    @Override
    public void unbind() {}
  }
}
