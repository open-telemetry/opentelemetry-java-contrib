/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import java.util.function.Supplier;

/**
 * State for a meter provider.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class MeterProviderSharedState {
  private final Supplier<MeterRegistry> meterRegistrySupplier;
  private final CallbackRegistrar callbackRegistrar;

  public MeterProviderSharedState(
      Supplier<MeterRegistry> meterRegistrySupplier, CallbackRegistrar callbackRegistrar) {
    this.meterRegistrySupplier = meterRegistrySupplier;
    this.callbackRegistrar = callbackRegistrar;
  }

  public MeterRegistry meterRegistry() {
    return meterRegistrySupplier.get();
  }

  public CallbackRegistration registerCallback(Runnable callback) {
    return callbackRegistrar.registerCallback(callback);
  }
}
