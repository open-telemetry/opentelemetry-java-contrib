/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.InstrumentState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import javax.annotation.Nullable;

abstract class AbstractInstrumentBuilder<BUILDER extends AbstractInstrumentBuilder<BUILDER>> {
  protected final MeterSharedState meterSharedState;
  protected final String name;
  @Nullable protected String description;
  @Nullable protected String unit;

  protected AbstractInstrumentBuilder(MeterSharedState meterSharedState, String name) {
    this.meterSharedState = meterSharedState;
    this.name = name;
  }

  protected AbstractInstrumentBuilder(
      MeterSharedState meterSharedState,
      String name,
      @Nullable String description,
      @Nullable String unit) {
    this.meterSharedState = meterSharedState;
    this.name = name;
    this.description = description;
    this.unit = unit;
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

  protected InstrumentState createInstrumentState() {
    return new InstrumentState(meterSharedState, name, description, unit);
  }
}
