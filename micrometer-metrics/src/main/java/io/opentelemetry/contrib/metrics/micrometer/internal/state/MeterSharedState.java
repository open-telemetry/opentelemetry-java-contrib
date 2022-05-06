/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.RegisteredCallback;
import javax.annotation.Nullable;

public final class MeterSharedState {
  private final MeterProviderSharedState providerSharedState;
  private final String instrumentationScopeName;
  @Nullable private final String instrumentationScopeVersion;
  @Nullable private final String schemaUrl;

  public MeterSharedState(
      MeterProviderSharedState providerSharedState,
      String instrumentationScopeName,
      @Nullable String instrumentationScopeVersion,
      @Nullable String schemaUrl) {
    this.providerSharedState = providerSharedState;
    this.instrumentationScopeName = instrumentationScopeName;
    this.instrumentationScopeVersion = instrumentationScopeVersion;
    this.schemaUrl = schemaUrl;
  }

  public MeterRegistry meterRegistry() {
    return providerSharedState.meterRegistry();
  }

  public String instrumentationScopeName() {
    return instrumentationScopeName;
  }

  @Nullable
  public String instrumentationScopeVersion() {
    return instrumentationScopeVersion;
  }

  @Nullable
  public String schemaUrl() {
    return schemaUrl;
  }

  public RegisteredCallback registerCallback(Runnable callback) {
    return providerSharedState.registerCallback(callback);
  }
}
