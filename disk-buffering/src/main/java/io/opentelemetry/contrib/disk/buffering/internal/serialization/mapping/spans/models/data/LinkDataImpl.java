package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.data.LinkData;

@AutoValue
public abstract class LinkDataImpl implements LinkData {

  public static Builder builder() {
    return new AutoValue_LinkDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSpanContext(SpanContext value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setTotalAttributeCount(Integer value);

    public abstract LinkDataImpl build();
  }
}
