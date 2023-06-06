package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.Constants;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;

public abstract class SumDataImpl<T extends PointData> extends DataImpl<T> implements SumData<T> {
  public Boolean monotonic = false;
  public AggregationTemporality aggregationTemporality = Constants.DEFAULT_AGGREGATION_TEMPORALITY;

  @Override
  public boolean isMonotonic() {
    return monotonic;
  }

  @Override
  public AggregationTemporality getAggregationTemporality() {
    return aggregationTemporality;
  }

  public static class LongData extends SumDataImpl<LongPointData> {}

  public static class DoubleData extends SumDataImpl<DoublePointData> {}
}
