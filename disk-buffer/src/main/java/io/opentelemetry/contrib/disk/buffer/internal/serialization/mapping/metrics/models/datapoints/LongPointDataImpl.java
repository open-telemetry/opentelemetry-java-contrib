package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.datapoints;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints.AutoValue_LongPointDataImpl;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import java.util.List;

@AutoValue
public abstract class LongPointDataImpl implements LongPointData {

  public static Builder builder() {
    return new AutoValue_LongPointDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder implements PointDataBuilder<Builder> {
    public abstract Builder setValue(Long value);

    public abstract Builder setExemplars(List<LongExemplarData> value);

    public abstract LongPointDataImpl build();
  }
}
