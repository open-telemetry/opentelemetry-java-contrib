/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

final class ObservableGaugeMeasurement
    implements ObservableLongMeasurement, ObservableDoubleMeasurement {
  private final SharedMeterState state;
  private final Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory;
  private final ConcurrentMap<Tags, NumberContainer> map;

  public ObservableGaugeMeasurement(
      SharedMeterState state, Function<Supplier<Number>, Gauge.Builder<Supplier<Number>>> factory) {
    this.state = state;
    this.factory = factory;
    this.map = new ConcurrentHashMap<>();
  }

  @Override
  public void observe(double value) {
    observe(value, Attributes.empty());
  }

  @Override
  public void observe(double value, Attributes attributes) {
    observeImpl(value, attributes);
  }

  @Override
  public void observe(long value) {
    observe(value, Attributes.empty());
  }

  @Override
  public void observe(long value, Attributes attributes) {
    observeImpl(value, attributes);
  }

  private void observeImpl(Number value, Attributes attributes) {
    Tags tags = TagUtils.attributesToTags(attributes);
    map.computeIfAbsent(
            tags,
            k -> {
              NumberContainer container = new NumberContainer(value);
              factory.apply(container).tags(tags).register(state.meterRegistry());
              return container;
            })
        .set(value);
  }

  private static class NumberContainer implements Supplier<Number> {
    private Number value;

    public NumberContainer(Number value) {
      this.value = value;
    }

    public void set(Number value) {
      this.value = value;
    }

    @Override
    public Number get() {
      return value;
    }
  }
}
