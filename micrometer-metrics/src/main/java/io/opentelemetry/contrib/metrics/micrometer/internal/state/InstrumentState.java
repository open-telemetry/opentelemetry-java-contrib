/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.RegisteredCallback;
import javax.annotation.Nullable;

public final class InstrumentState {
  private final MeterSharedState meterSharedState;
  private final String name;
  @Nullable private final String description;
  @Nullable private final String unit;

  public InstrumentState(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    this.meterSharedState = meterSharedState;
    this.name = name;
    this.description = description;
    this.unit = unit;
  }

  public MeterRegistry meterRegistry() {
    return meterSharedState.meterRegistry();
  }

  public RegisteredCallback registerCallback(Runnable runnable) {
    return meterSharedState.registerCallback(runnable);
  }

  public String name() {
    return name;
  }

  @Nullable
  public String description() {
    return description;
  }

  @Nullable
  public String unit() {
    return unit;
  }
}
