/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.RegisteredCallback;
import java.util.List;

public final class MeterProviderSharedState {
  private final MeterRegistry meterRegistry;
  private final List<Runnable> callbacks;

  public MeterProviderSharedState(MeterRegistry meterRegistry, List<Runnable> callbacks) {
    this.meterRegistry = meterRegistry;
    this.callbacks = callbacks;
  }

  public MeterRegistry meterRegistry() {
    return meterRegistry;
  }

  public RegisteredCallback registerCallback(Runnable callback) {
    callbacks.add(callback);
    return () -> callbacks.remove(callback);
  }
}
