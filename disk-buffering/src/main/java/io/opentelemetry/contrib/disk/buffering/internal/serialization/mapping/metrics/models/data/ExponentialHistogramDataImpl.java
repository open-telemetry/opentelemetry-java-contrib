package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.AggregationTemporalityBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.DataBuilder;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;

@AutoValue
public abstract class ExponentialHistogramDataImpl implements ExponentialHistogramData {

  public static Builder builder() {
    return new AutoValue_ExponentialHistogramDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements DataBuilder<ExponentialHistogramPointData, Builder>,
          AggregationTemporalityBuilder<Builder> {
    public abstract ExponentialHistogramDataImpl build();
  }
}
