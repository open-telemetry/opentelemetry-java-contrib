package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;

public interface AggregationTemporalityBuilder<T extends AggregationTemporalityBuilder<?>> {

  @CanIgnoreReturnValue
  T setAggregationTemporality(AggregationTemporality value);
}
