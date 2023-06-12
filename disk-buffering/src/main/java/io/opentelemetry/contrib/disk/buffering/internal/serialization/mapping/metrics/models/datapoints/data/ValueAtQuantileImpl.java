/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;

@AutoValue
public abstract class ValueAtQuantileImpl implements ValueAtQuantile {

  public static Builder builder() {
    return new AutoValue_ValueAtQuantileImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setQuantile(Double value);

    public abstract Builder setValue(Double value);

    public abstract ValueAtQuantileImpl build();
  }
}
