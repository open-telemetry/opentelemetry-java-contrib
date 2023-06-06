package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;

@AutoValue
public abstract class ExponentialHistogramDataImpl implements ExponentialHistogramData {

  public static Builder builder() {
    return new AutoValue_ExponentialHistogramDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements DataBuilder<ExponentialHistogramPointData, Builder> {
    public abstract Builder setAggregationTemporality(AggregationTemporality value);

    public abstract ExponentialHistogramDataImpl build();
  }
}
