/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Implementation of a {@link CallbackRegistrar} that uses a Micrometer {@link Meter} to invoke the
 * callbacks when the meter is measured.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class PollingMeterCallbackRegistrar implements CallbackRegistrar {
  static final String OTEL_POLLING_METER_NAME = "otel_polling_meter";

  private final Supplier<MeterRegistry> meterRegistrySupplier;
  private final List<Runnable> callbacks;
  @Nullable private volatile Meter pollingMeter;

  public PollingMeterCallbackRegistrar(Supplier<MeterRegistry> meterRegistrySupplier) {
    this.meterRegistrySupplier = meterRegistrySupplier;
    this.callbacks = new CopyOnWriteArrayList<>();
  }

  private void poll() {
    for (Runnable callback : this.callbacks) {
      callback.run();
    }
  }

  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    if (callback != null) {
      ensurePollingMeterCreated();
      callbacks.add(callback);
      return () -> callbacks.remove(callback);
    } else {
      return () -> {};
    }
  }

  private synchronized void ensurePollingMeterCreated() {
    if (pollingMeter == null) {
      pollingMeter = createPollingMeter(meterRegistrySupplier.get());
    }
  }

  /**
   * Creates a dummy {@link Meter} which will be used to intercept when measurements are being
   * enumerated so that observable instruments can be polled to record their metrics.
   */
  private Meter createPollingMeter(MeterRegistry meterRegistry) {
    return Meter.builder(OTEL_POLLING_METER_NAME, Meter.Type.OTHER, PollingIterable.of(this::poll))
        .register(meterRegistry);
  }

  @Override
  public synchronized void close() {
    if (pollingMeter != null) {
      meterRegistrySupplier.get().remove(pollingMeter);
      pollingMeter = null;
    }
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

    PollingIterable(Runnable callback) {
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
