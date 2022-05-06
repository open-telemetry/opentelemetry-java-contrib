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
import java.util.NoSuchElementException;

public final class MicrometerMeterProvider implements MeterProvider, AutoCloseable {
  private final MeterProviderSharedState meterProviderSharedState;
  private final Meter pollingMeter;

  public MicrometerMeterProvider(MeterRegistry meterRegistry) {
    this.meterProviderSharedState = new MeterProviderSharedState(meterRegistry);

    this.pollingMeter =
        Meter.builder("otel_polling_meter", Meter.Type.OTHER, PollingIterable.of(this::poll))
            .register(meterRegistry);
  }

  private void poll() {
    for (Runnable callback : meterProviderSharedState.callbacks()) {
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
