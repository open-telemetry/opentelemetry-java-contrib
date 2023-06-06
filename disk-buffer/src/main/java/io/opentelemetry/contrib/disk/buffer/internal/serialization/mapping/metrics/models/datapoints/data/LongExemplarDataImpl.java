package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.datapoints.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints.data.AutoValue_LongExemplarDataImpl;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;

@AutoValue
public abstract class LongExemplarDataImpl implements LongExemplarData {

  public static Builder builder() {
    return new AutoValue_LongExemplarDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder implements ExemplarDataBuilder<Builder> {
    public abstract Builder setValue(Long value);

    public abstract LongExemplarDataImpl build();
  }
}
