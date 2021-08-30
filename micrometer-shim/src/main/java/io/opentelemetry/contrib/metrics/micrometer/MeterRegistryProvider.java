/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.MeterProvider;
import java.util.Objects;
import java.util.function.Supplier;

public final class MeterRegistryProvider implements MeterProvider {
  private final Supplier<MeterRegistry> meterRegistrySupplier;

  private MeterRegistryProvider(Supplier<MeterRegistry> meterRegistrySupplier) {
    this.meterRegistrySupplier = meterRegistrySupplier;
  }

  public static MeterRegistryProvider create(MeterRegistry meterRegistry) {
    Objects.requireNonNull(meterRegistry);
    return new MeterRegistryProvider(() -> meterRegistry);
  }

  public static MeterRegistryProvider create(Supplier<MeterRegistry> meterRegistrySupplier) {
    Objects.requireNonNull(meterRegistrySupplier);
    return new MeterRegistryProvider(meterRegistrySupplier);
  }

  @Override
  public MeterBuilder meterBuilder(String instrumentationName) {
    return MeterRegistryMeter.newBuilder(meterRegistrySupplier, instrumentationName);
  }
}
