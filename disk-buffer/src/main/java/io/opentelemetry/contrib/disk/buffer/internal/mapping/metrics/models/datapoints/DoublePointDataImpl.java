package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import java.util.List;

@AutoValue
public abstract class DoublePointDataImpl implements DoublePointData {

  public static Builder builder() {
    return new AutoValue_DoublePointDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder implements PointDataBuilder<Builder> {
    public abstract Builder setValue(Double value);

    public abstract Builder setExemplars(List<DoubleExemplarData> value);

    public abstract DoublePointDataImpl build();
  }
}
