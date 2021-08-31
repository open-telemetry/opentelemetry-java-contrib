/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundLongCounter;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.context.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class LongCounter implements io.opentelemetry.api.metrics.LongCounter {
  private final SharedMeterState state;
  private final Reader<Counter.Builder> factory;
  private final Map<Attributes, Counter> map;

  private LongCounter(SharedMeterState state, Reader<Counter.Builder> factory) {
    this.state = state;
    this.factory = factory;
    this.map = new ConcurrentHashMap<>();
  }

  @Override
  public void add(long value) {
    add(value, Attributes.empty(), Context.current());
  }

  @Override
  public void add(long value, Attributes attributes) {
    add(value, attributes, Context.current());
  }

  @Override
  public void add(long value, Attributes attributes, Context context) {
    BoundLongCounter bound = bind(attributes);
    try {
      bound.add(value, context);
    } finally {
      bound.unbind();
    }
  }

  @Override
  public BoundLongCounter bind(Attributes attributes) {
    Counter counter = map.computeIfAbsent(attributes, this::createCounter);
    return new Bound(counter);
  }

  private Counter createCounter(Attributes attributes) {
    Iterable<Tag> tags = TagUtils.attributesToTags(attributes);
    return factory.get()
            .tags(tags)
            .register(state.meterRegistry());
  }

  static LongCounterBuilder newBuilder(SharedMeterState state, String name) {
    return new Builder(state, () -> Counter.builder(name));
  }

  static LongCounterBuilder newBuilder(SharedMeterState state, Reader<Counter.Builder> factory) {
    return new Builder(state, factory);
  }

  static final class Builder implements LongCounterBuilder {
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
    public DoubleCounterBuilder ofDoubles() {
      return DoubleCounter.newBuilder(state, factory);
    }

    @Override
    public LongCounter build() {
      return new LongCounter(state, factory);
    }

    @Override
    public void buildWithCallback(Consumer<ObservableLongMeasurement> callback) {
      LongCounter counter = build();
      state.registerCallback(() -> callback.accept(
              new ObservableLongMeasurement() {
                @Override
                public void observe(long value) {
                  counter.add(value);
                }

                @Override
                public void observe(long value, Attributes attributes) {
                  counter.add(value, attributes);
                }
              }));
    }
  }

  static final class Bound implements BoundLongCounter {
    private final Counter counter;

    Bound(Counter counter) {
      this.counter = counter;
    }

    @Override
    public void add(long value) {
      add(value, Context.current());
    }

    @Override
    public void add(long value, Context context) {
      counter.increment(value);
    }

    @Override
    public void unbind() {}
  }
}
