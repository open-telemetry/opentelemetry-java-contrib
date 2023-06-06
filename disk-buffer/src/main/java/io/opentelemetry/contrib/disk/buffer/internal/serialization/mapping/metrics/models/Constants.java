package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models;

import io.opentelemetry.sdk.metrics.data.AggregationTemporality;

public final class Constants {
  public static final AggregationTemporality DEFAULT_AGGREGATION_TEMPORALITY =
      AggregationTemporality.DELTA;

  private Constants() {}
}
