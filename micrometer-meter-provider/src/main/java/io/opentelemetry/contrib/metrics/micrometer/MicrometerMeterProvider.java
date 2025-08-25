/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static java.util.Objects.requireNonNull;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.metrics.micrometer.internal.MemoizingSupplier;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import java.util.function.Supplier;

/**
 * An implementation of {@link MeterProvider} that delegates metrics to a Micrometer {@link
 * MeterRegistry}.
 */
public final class MicrometerMeterProvider implements MeterProvider, AutoCloseable {

  private final MeterProviderSharedState meterProviderSharedState;
  private final CallbackRegistrar callbackRegistrar;

  /**
   * Creates a new instance of {@link MicrometerMeterProvider} for the provided {@link
   * MeterRegistry}.
   *
   * @param meterRegistrySupplier supplies the {@link MeterRegistry}
   */
  MicrometerMeterProvider(
      Supplier<MeterRegistry> meterRegistrySupplier, CallbackRegistrar callbackRegistrar) {
    this.callbackRegistrar = callbackRegistrar;
    this.meterProviderSharedState =
        new MeterProviderSharedState(meterRegistrySupplier, callbackRegistrar);
  }

  /** Closes the current provider. */
  @Override
  public void close() {
    callbackRegistrar.close();
  }

  /** {@inheritDoc} */
  @Override
  public MeterBuilder meterBuilder(String instrumentationScopeName) {
    requireNonNull(instrumentationScopeName, "instrumentationScopeName");
    return new MicrometerMeterBuilder(meterProviderSharedState, instrumentationScopeName);
  }

  /** Returns a new builder instance for this provider with the specified {@link MeterRegistry}. */
  public static MicrometerMeterProviderBuilder builder(MeterRegistry meterRegistry) {
    requireNonNull(meterRegistry, "meterRegistry");
    return new MicrometerMeterProviderBuilder(() -> meterRegistry);
  }

  /**
   * Returns a new builder instance for this provider with a {@link Supplier} for a {@link
   * MeterRegistry}.
   *
   * <p>This method should be used when the {@link MeterRegistry} must be lazily initialized.
   */
  public static MicrometerMeterProviderBuilder builder(
      Supplier<MeterRegistry> meterRegistrySupplier) {
    requireNonNull(meterRegistrySupplier, "meterRegistrySupplier");
    return new MicrometerMeterProviderBuilder(new MemoizingSupplier<>(meterRegistrySupplier));
  }
}
