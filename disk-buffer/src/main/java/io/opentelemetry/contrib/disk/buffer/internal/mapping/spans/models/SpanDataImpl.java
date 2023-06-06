package io.opentelemetry.contrib.disk.buffer.internal.mapping.spans.models;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;

@AutoValue
public abstract class SpanDataImpl implements SpanData {

  public static Builder builder() {
    return new AutoValue_SpanDataImpl.Builder();
  }

  @Override
  public boolean hasEnded() {
    return true;
  }

  @SuppressWarnings(
      "deprecation") // Overridden to avoid AutoValue to generate builder method for it.
  @Override
  public io.opentelemetry.sdk.common.InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public abstract InstrumentationScopeInfo getInstrumentationScopeInfo();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String value);

    public abstract Builder setKind(SpanKind value);

    public abstract Builder setSpanContext(SpanContext value);

    public abstract Builder setParentSpanContext(SpanContext value);

    public abstract Builder setStatus(StatusData value);

    public abstract Builder setStartEpochNanos(Long value);

    public abstract Builder setTotalAttributeCount(Integer value);

    public abstract Builder setTotalRecordedEvents(Integer value);

    public abstract Builder setTotalRecordedLinks(Integer value);

    public abstract Builder setEndEpochNanos(Long value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setEvents(List<EventData> value);

    public abstract Builder setLinks(List<LinkData> value);

    public abstract Builder setInstrumentationScopeInfo(InstrumentationScopeInfo value);

    public abstract Builder setResource(Resource value);

    public abstract SpanDataImpl build();
  }
}
