package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;

@AutoValue
public abstract class EventDataImpl implements EventData {

  public static Builder builder() {
    return new AutoValue_EventDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setEpochNanos(Long value);

    public abstract Builder setTotalAttributeCount(Integer value);

    public abstract EventDataImpl build();
  }
}
