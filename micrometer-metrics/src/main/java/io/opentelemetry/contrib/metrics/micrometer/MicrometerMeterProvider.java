/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import java.util.Objects;

/**
 * An implementation of {@link MeterProvider} that delegates metrics to a Micrometer {@link
 * MeterRegistry}.
 */
public final class MicrometerMeterProvider implements MeterProvider, AutoCloseable {
  static final String OTEL_POLLING_METER_NAME = "otel_polling_meter";

  private final MeterProviderSharedState meterProviderSharedState;
  private final CallbackRegistrar callbackRegistrar;

  /**
   * Creates a new instance of {@link MicrometerMeterProvider} for the provided {@link
   * MeterRegistry}.
   *
   * @param meterRegistry the {@link MeterRegistry}
   */
  MicrometerMeterProvider(MeterRegistry meterRegistry, CallbackRegistrar callbackRegistrar) {
    this.callbackRegistrar = callbackRegistrar;
    this.meterProviderSharedState = new MeterProviderSharedState(meterRegistry, callbackRegistrar);
  }

  @Override
  public void close() {
    callbackRegistrar.close();
  }

  /** {@inheritDoc} */
  @Override
  public MeterBuilder meterBuilder(String instrumentationScopeName) {
    Objects.requireNonNull(instrumentationScopeName, "instrumentationScopeName");
    return new MicrometerMeterBuilder(meterProviderSharedState, instrumentationScopeName);
  }

  public static MicrometerMeterProviderBuilder builder(MeterRegistry meterRegistry) {
    Objects.requireNonNull(meterRegistry, "meterRegistry");
    return new MicrometerMeterProviderBuilder(meterRegistry);
  }
}
