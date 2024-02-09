/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.List;
import javax.annotation.Nullable;

abstract class AbstractInstrumentBuilder<BUILDER extends AbstractInstrumentBuilder<BUILDER>> {
  protected final MeterSharedState meterSharedState;
  protected final String name;
  @Nullable protected String description;
  @Nullable protected String unit;
  @Nullable protected List<AttributeKey<?>> attributes;
  @Nullable protected List<? extends Number> explicitBucketBoundaries;

  protected AbstractInstrumentBuilder(MeterSharedState meterSharedState, String name) {
    this.meterSharedState = meterSharedState;
    this.name = name;
  }

  protected AbstractInstrumentBuilder(AbstractInstrumentBuilder<?> parent) {
    this.meterSharedState = parent.meterSharedState;
    this.name = parent.name;
    this.description = parent.description;
    this.unit = parent.unit;
    this.attributes = parent.attributes;
    this.explicitBucketBoundaries = parent.explicitBucketBoundaries;
  }

  protected abstract BUILDER self();

  @CanIgnoreReturnValue
  public BUILDER setDescription(String description) {
    this.description = description;
    return self();
  }

  @CanIgnoreReturnValue
  public BUILDER setUnit(String unit) {
    this.unit = unit;
    return self();
  }

  @CanIgnoreReturnValue
  public BUILDER setAttributesAdvice(List<AttributeKey<?>> attributes) {
    this.attributes = attributes;
    return self();
  }

  @CanIgnoreReturnValue
  public BUILDER setExplicitBucketBoundaries(List<? extends Number> explicitBucketBoundaries) {
    this.explicitBucketBoundaries = explicitBucketBoundaries;
    return self();
  }

  protected InstrumentState createInstrumentState() {
    return new InstrumentState(
        meterSharedState, name, description, unit, attributes, explicitBucketBoundaries);
  }
}
