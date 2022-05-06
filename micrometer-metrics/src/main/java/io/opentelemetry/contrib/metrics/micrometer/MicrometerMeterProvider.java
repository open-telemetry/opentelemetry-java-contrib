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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An implementation of {@link MeterProvider} that delegates metrics to a Micrometer {@link
 * MeterRegistry}.
 */
public final class MicrometerMeterProvider implements MeterProvider, AutoCloseable {
  static final String OTEL_POLLING_METER_NAME = "otel_polling_meter";

  private final MeterProviderSharedState meterProviderSharedState;
  private final List<Runnable> callbacks;
  private final Meter pollingMeter;

  /**
   * Creates a new instance of {@link MicrometerMeterProvider} for the provided {@link
   * MeterRegistry}.
   *
   * @param meterRegistry the {@link MeterRegistry}
   */
  public MicrometerMeterProvider(MeterRegistry meterRegistry) {
    this(meterRegistry, new CopyOnWriteArrayList<>());
  }

  // for testing purposes
  MicrometerMeterProvider(MeterRegistry meterRegistry, List<Runnable> callbacks) {
    Objects.requireNonNull(meterRegistry, "meterRegistry");
    this.meterProviderSharedState = new MeterProviderSharedState(meterRegistry, callbacks);
    this.callbacks = callbacks;
    this.pollingMeter = createPollingMeter(meterRegistry);
  }

  /**
   * Creates a dummy {@link Meter} which will be used to intercept when measurements are being
   * enumerated so that observable instruments can be polled to record their metrics.
   */
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

  /** {@inheritDoc} */
  @Override
  public MeterBuilder meterBuilder(String instrumentationScopeName) {
    Objects.requireNonNull(instrumentationScopeName, "instrumentationScopeName");
    return new MicrometerMeterBuilder(meterProviderSharedState, instrumentationScopeName);
  }

  /**
   * An implementation of an {@link Iterable Iterable<T>} that will invoke a {@link Runnable} when
   * it is enumerated.
   */
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
