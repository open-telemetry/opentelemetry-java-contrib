package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface MonotonicBuilder<T extends MonotonicBuilder<?>> {

  @CanIgnoreReturnValue
  T setMonotonic(Boolean value);
}
