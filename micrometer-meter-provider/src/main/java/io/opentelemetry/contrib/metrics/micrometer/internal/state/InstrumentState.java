/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.state;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import java.util.List;
import javax.annotation.Nullable;

/**
 * State for an instrument.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class InstrumentState {
  private final MeterSharedState meterSharedState;
  private final String name;
  @Nullable private final String description;
  @Nullable private final String unit;
  @Nullable private final List<AttributeKey<?>> attributes;
  @Nullable private final List<? extends Number> explicitBucketBoundaries;

  public InstrumentState(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit,
      @Nullable List<AttributeKey<?>> attributes,
      @Nullable List<? extends Number> explicitBucketBoundaries) {
    this.meterSharedState = meterSharedState;
    this.name = name;
    this.description = description;
    this.unit = unit;
    this.attributes = attributes;
    this.explicitBucketBoundaries = explicitBucketBoundaries;
  }

  public MeterRegistry meterRegistry() {
    return meterSharedState.meterRegistry();
  }

  public Tag instrumentationScopeNameTag() {
    return meterSharedState.instrumentationScopeNameTag();
  }

  public Tag instrumentationScopeVersionTag() {
    return meterSharedState.instrumentationScopeVersionTag();
  }

  public CallbackRegistration registerCallback(Runnable runnable) {
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

  @Nullable
  public List<AttributeKey<?>> attributesAdvice() {
    return attributes;
  }

  @Nullable
  public List<? extends Number> explicitBucketBoundariesAdvice() {
    return explicitBucketBoundaries;
  }
}
