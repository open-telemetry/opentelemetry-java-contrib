package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.Attributes;

public interface PointDataBuilder<T extends PointDataBuilder<?>> {

  @CanIgnoreReturnValue
  T setStartEpochNanos(Long value);

  @CanIgnoreReturnValue
  T setEpochNanos(Long value);

  @CanIgnoreReturnValue
  T setAttributes(Attributes value);
}
