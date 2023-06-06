package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
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
    public abstract static class Builder implements DataBuilder<LongPointData, Builder> {
      public abstract Builder setMonotonic(Boolean value);

      public abstract Builder setAggregationTemporality(AggregationTemporality value);

      public abstract LongData build();
    }
  }

  @AutoValue
  public abstract static class DoubleData extends SumDataImpl<DoublePointData> {

    public static Builder builder() {
      return new AutoValue_SumDataImpl_DoubleData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements DataBuilder<DoublePointData, Builder> {
      public abstract Builder setMonotonic(Boolean value);

      public abstract Builder setAggregationTemporality(AggregationTemporality value);

      public abstract DoubleData build();
    }
  }
}
