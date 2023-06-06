package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.datapoints.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints.data.AutoValue_DoubleExemplarDataImpl;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;

@AutoValue
public abstract class DoubleExemplarDataImpl implements DoubleExemplarData {

  public static Builder builder() {
    return new AutoValue_DoubleExemplarDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder implements ExemplarDataBuilder<Builder> {
    public abstract Builder setValue(Double value);

    public abstract DoubleExemplarDataImpl build();
  }
}
