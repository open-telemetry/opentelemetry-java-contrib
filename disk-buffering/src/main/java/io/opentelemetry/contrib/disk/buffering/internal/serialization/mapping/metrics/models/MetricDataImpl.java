/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;

@AutoValue
public abstract class MetricDataImpl implements MetricData {

  public static Builder builder() {
    return new AutoValue_MetricDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setResource(Resource value);

    public abstract Builder setInstrumentationScopeInfo(InstrumentationScopeInfo value);

    public abstract Builder setName(String value);

    public abstract Builder setDescription(String value);

    public abstract Builder setUnit(String value);

    public abstract Builder setType(MetricDataType value);

    public abstract Builder setData(Data<?> value);

    public abstract MetricDataImpl build();
  }
}
