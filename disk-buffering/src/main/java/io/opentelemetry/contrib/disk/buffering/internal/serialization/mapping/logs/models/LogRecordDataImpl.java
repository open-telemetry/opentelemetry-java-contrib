package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;

@AutoValue
public abstract class LogRecordDataImpl implements LogRecordData {

  public static Builder builder() {
    return new AutoValue_LogRecordDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setResource(Resource value);

    public abstract Builder setInstrumentationScopeInfo(InstrumentationScopeInfo value);

    public abstract Builder setTimestampEpochNanos(Long value);

    public abstract Builder setObservedTimestampEpochNanos(Long value);

    public abstract Builder setSpanContext(SpanContext value);

    public abstract Builder setSeverity(Severity value);

    public abstract Builder setSeverityText(String value);

    public abstract Builder setBody(Body value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setTotalAttributeCount(Integer value);

    public abstract LogRecordDataImpl build();
  }
}
