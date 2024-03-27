/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.common.util.StringUtils;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import javax.annotation.Nullable;

/**
 * State for a meter.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class MeterSharedState {
  private final MeterProviderSharedState providerSharedState;
  private final Tag instrumentationScopeNameTag;
  private final Tag instrumentationScopeVersionTag;
  @Nullable private final String schemaUrl;

  public MeterSharedState(
      MeterProviderSharedState providerSharedState,
      String instrumentationScopeName,
      @Nullable String instrumentationScopeVersion,
      @Nullable String schemaUrl) {

    this.providerSharedState = providerSharedState;
    this.instrumentationScopeNameTag =
        Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, instrumentationScopeName);
    if (StringUtils.isNotBlank(instrumentationScopeVersion)) {
      this.instrumentationScopeVersionTag =
          Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, instrumentationScopeVersion);
    } else {
      this.instrumentationScopeVersionTag = Constants.UNKNOWN_INSTRUMENTATION_VERSION_TAG;
    }
    this.schemaUrl = schemaUrl;
  }

  public MeterRegistry meterRegistry() {
    return providerSharedState.meterRegistry();
  }

  public Tag instrumentationScopeNameTag() {
    return instrumentationScopeNameTag;
  }

  public Tag instrumentationScopeVersionTag() {
    return instrumentationScopeVersionTag;
  }

  @Nullable
  public String schemaUrl() {
    return schemaUrl;
  }

  public CallbackRegistration registerCallback(Runnable callback) {
    return providerSharedState.registerCallback(callback);
  }
}
