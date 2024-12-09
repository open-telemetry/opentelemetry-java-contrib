/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

@AutoValue
public abstract class LogRecordDataImpl implements LogRecordData {

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
  @Nullable
  public abstract Value<?> getBodyValue();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setResource(Resource value);

    public abstract Builder setInstrumentationScopeInfo(InstrumentationScopeInfo value);

    public abstract Builder setTimestampEpochNanos(Long value);

    public abstract Builder setObservedTimestampEpochNanos(Long value);

    public abstract Builder setSpanContext(SpanContext value);

    public abstract Builder setSeverity(Severity value);

    public abstract Builder setSeverityText(String value);

    @Deprecated
    @CanIgnoreReturnValue
    public Builder setBody(io.opentelemetry.sdk.logs.data.Body body) {
      if (body.getType() == io.opentelemetry.sdk.logs.data.Body.Type.STRING) {
        setBodyValue(Value.of(body.asString()));
      } else if (body.getType() == io.opentelemetry.sdk.logs.data.Body.Type.EMPTY) {
        setBodyValue(null);
      }
      return this;
    }

    public abstract Builder setBodyValue(@Nullable Value<?> value);

    public abstract Builder setAttributes(Attributes value);

    public abstract Builder setTotalAttributeCount(Integer value);

    public abstract LogRecordDataImpl build();
  }
}
