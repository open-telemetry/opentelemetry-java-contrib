/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import javax.annotation.Nullable;

final class MicrometerMeterBuilder implements MeterBuilder {
  private final MeterProviderSharedState meterProviderSharedState;
  private final String instrumentationScopeName;
  @Nullable private String instrumentationScopeVersion;
  @Nullable private String schemaUrl;

  MicrometerMeterBuilder(
      MeterProviderSharedState meterProviderSharedState, String instrumentationScopeName) {
    this.meterProviderSharedState = meterProviderSharedState;
    this.instrumentationScopeName = instrumentationScopeName;
  }

  @Override
  @CanIgnoreReturnValue
  public MeterBuilder setSchemaUrl(String schemaUrl) {
    this.schemaUrl = schemaUrl;
    return this;
  }

  @Override
  @CanIgnoreReturnValue
  public MeterBuilder setInstrumentationVersion(String instrumentationScopeVersion) {
    this.instrumentationScopeVersion = instrumentationScopeVersion;
    return this;
  }

  @Override
  public Meter build() {
    MeterSharedState state =
        new MeterSharedState(
            meterProviderSharedState,
            instrumentationScopeName,
            instrumentationScopeVersion,
            schemaUrl);
    return new MicrometerMeter(state);
  }
}
