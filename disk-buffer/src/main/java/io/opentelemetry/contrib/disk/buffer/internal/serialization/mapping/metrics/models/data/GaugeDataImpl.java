package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;

public abstract class GaugeDataImpl<T extends PointData> implements GaugeData<T> {

  @AutoValue
  public abstract static class LongData extends GaugeDataImpl<LongPointData> {

    public static Builder builder() {
      return new AutoValue_GaugeDataImpl_LongData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements DataBuilder<LongPointData, Builder> {
      public abstract LongData build();
    }
  }

  @AutoValue
  public abstract static class DoubleData extends GaugeDataImpl<DoublePointData> {

    public static Builder builder() {
      return new AutoValue_GaugeDataImpl_DoubleData.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements DataBuilder<DoublePointData, Builder> {
      public abstract DoubleData build();
    }
  }
}
