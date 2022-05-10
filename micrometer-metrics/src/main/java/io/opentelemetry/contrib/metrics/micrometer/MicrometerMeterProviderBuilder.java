/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.internal.PollingMeterCallbackRegistrar;
import javax.annotation.Nullable;

/** Builder utility class for creating instances of {@link MicrometerMeterProvider}. */
public class MicrometerMeterProviderBuilder {
  private final MeterRegistry meterRegistry;
  @Nullable private CallbackRegistrar callbackRegistrar;

  MicrometerMeterProviderBuilder(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public MicrometerMeterProviderBuilder setCallbackRegistrar(CallbackRegistrar callbackRegistrar) {
    this.callbackRegistrar = callbackRegistrar;
    return this;
  }

  /**
   * Constructs a new instance of the provider based on the builder's values.
   *
   * @return a new provider's instance.
   */
  public MicrometerMeterProvider build() {
    CallbackRegistrar callbackRegistrar = this.callbackRegistrar;
    if (callbackRegistrar == null) {
      callbackRegistrar = new PollingMeterCallbackRegistrar(meterRegistry);
    }
    return new MicrometerMeterProvider(meterRegistry, callbackRegistrar);
  }
}
