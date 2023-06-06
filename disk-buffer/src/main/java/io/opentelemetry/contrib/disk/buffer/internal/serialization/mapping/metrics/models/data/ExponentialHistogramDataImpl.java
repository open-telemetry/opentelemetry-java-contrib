package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.Constants;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;

public final class ExponentialHistogramDataImpl extends DataImpl<ExponentialHistogramPointData>
    implements ExponentialHistogramData {
  public AggregationTemporality aggregationTemporality = Constants.DEFAULT_AGGREGATION_TEMPORALITY;

  @Override
  public AggregationTemporality getAggregationTemporality() {
    return aggregationTemporality;
  }
}
