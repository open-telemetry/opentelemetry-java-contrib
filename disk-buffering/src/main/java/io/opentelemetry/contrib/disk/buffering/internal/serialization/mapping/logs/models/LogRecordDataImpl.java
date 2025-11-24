/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.common.ExtendedAttributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

@AutoValue
public abstract class LogRecordDataImpl implements ExtendedLogRecordData {

  public static Builder builder() {
    return new AutoValue_LogRecordDataImpl.Builder();
  }

  @Deprecated
  public io.opentelemetry.sdk.logs.data.Body getBody() {
    Value<?> valueBody = getBodyValue();
    return valueBody == null
        ? io.opentelemetry.sdk.logs.data.Body.empty()
        : io.opentelemetry.sdk.logs.data.Body.string(valueBody.asString());
  }

  @Override
  public ExtendedAttributes getExtendedAttributes() {
    return ExtendedAttributes.builder().putAll(getAttributes()).build();
  }

  // It's only deprecated in the incubating interface for extended attributes, which are not yet
  // supported in this module.
  @SuppressWarnings("deprecation")
  @Override
  public abstract Attributes getAttributes();

  @Override
  @Nullable
  public abstract Value<?> getBodyValue();

  @Override
  @Nullable
  public abstract String getEventName();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setResource(Resource value);

    public abstract Builder setInstrumentationScopeInfo(InstrumentationScopeInfo value);

    public abstract Builder setTimestampEpochNanos(long value);

    public abstract Builder setObservedTimestampEpochNanos(long value);

    public abstract Builder setSpanContext(SpanContext value);

    public abstract Builder setSeverity(Severity value);

    public abstract Builder setSeverityText(String value);

    public abstract Builder setBodyValue(@Nullable Value<?> value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setTotalAttributeCount(int value);

    public abstract Builder setEventName(String value);

    public abstract LogRecordDataImpl build();
  }
}
