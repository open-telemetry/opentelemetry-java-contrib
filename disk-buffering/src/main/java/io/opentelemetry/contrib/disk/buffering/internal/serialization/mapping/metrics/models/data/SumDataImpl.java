/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.DataBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.AggregationTemporalityBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.MonotonicBuilder;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;

public abstract class SumDataImpl<T extends PointData> implements SumData<T> {
  @AutoValue
  public abstract static class LongData extends SumDataImpl<LongPointData> {

    public static Builder builder() {
      return new AutoValue_SumDataImpl_LongData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder
        implements DataBuilder<LongPointData, Builder>,
            AggregationTemporalityBuilder<Builder>,
            MonotonicBuilder<Builder> {
      public abstract LongData build();
    }
  }

  @AutoValue
  public abstract static class DoubleData extends SumDataImpl<DoublePointData> {

    public static Builder builder() {
      return new AutoValue_SumDataImpl_DoubleData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder
        implements DataBuilder<DoublePointData, Builder>,
            AggregationTemporalityBuilder<Builder>,
            MonotonicBuilder<Builder> {
      public abstract DoubleData build();
    }
  }
}
