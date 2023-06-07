package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;

public interface ExemplarDataBuilder<T extends ExemplarDataBuilder<?>> {

  @CanIgnoreReturnValue
  T setSpanContext(SpanContext value);

  @CanIgnoreReturnValue
  T setEpochNanos(Long value);

  @CanIgnoreReturnValue
  T setFilteredAttributes(Attributes value);
}
