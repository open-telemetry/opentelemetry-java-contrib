/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;

final class SharedMeterState {
  private final Supplier<MeterRegistry> meterRegistrySupplier;
  private final String instrumentationName;
  private final String intrumentationVersion;
  private final String schemaUrl;

  public SharedMeterState(
      Supplier<MeterRegistry> meterRegistrySupplier,
      String instrumentationName,
      String intrumentationVersion,
      String schemaUrl) {
    this.meterRegistrySupplier = meterRegistrySupplier;
    this.instrumentationName = instrumentationName;
    this.intrumentationVersion = intrumentationVersion;
    this.schemaUrl = schemaUrl;
  }

  public MeterRegistry meterRegistry() {
    return meterRegistrySupplier.get();
  }

  public String instrumentationName() {
    return instrumentationName;
  }

  public String intrumentationVersion() {
    return intrumentationVersion;
  }

  public String schemaUrl() {
    return schemaUrl;
  }
}
