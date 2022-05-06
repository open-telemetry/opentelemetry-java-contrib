/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MicrometerMeterProvider implements MeterProvider, AutoCloseable {
  static final String OTEL_POLLING_METER_NAME = "otel_polling_meter";

  private final MeterProviderSharedState meterProviderSharedState;
  private final List<Runnable> callbacks;
  private final Meter pollingMeter;

  public MicrometerMeterProvider(MeterRegistry meterRegistry) {
    this(meterRegistry, new CopyOnWriteArrayList<>());
  }

  MicrometerMeterProvider(MeterRegistry meterRegistry, List<Runnable> callbacks) {
    this.meterProviderSharedState = new MeterProviderSharedState(meterRegistry, callbacks);
    this.callbacks = callbacks;
    this.pollingMeter = createPollingMeter(meterRegistry);
  }

  private Meter createPollingMeter(MeterRegistry meterRegistry) {
    return Meter.builder(OTEL_POLLING_METER_NAME, Meter.Type.OTHER, PollingIterable.of(this::poll))
        .register(meterRegistry);
  }

  private void poll() {
    for (Runnable callback : this.callbacks) {
      callback.run();
    }
  }

  @Override
  public void close() {
    meterProviderSharedState.meterRegistry().remove(pollingMeter);
  }

  @Override
  public MeterBuilder meterBuilder(String instrumentationScopeName) {
    return new MicrometerMeterBuilder(meterProviderSharedState, instrumentationScopeName);
  }

  @SuppressWarnings("IterableAndIterator")
  private static class PollingIterable<T> implements Iterable<T>, Iterator<T> {

    static <T> Iterable<T> of(Runnable callback) {
      return new PollingIterable<>(callback);
    }

    private final Runnable callback;

    public PollingIterable(Runnable callback) {
      this.callback = callback;
    }

    @Override
    public Iterator<T> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      callback.run();
      return false;
    }

    @Override
    public T next() {
      throw new NoSuchElementException();
    }
  }
}
