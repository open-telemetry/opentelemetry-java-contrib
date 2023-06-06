package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.data;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.Constants;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;

public class HistogramDataImpl extends DataImpl<HistogramPointData> implements HistogramData {
  public AggregationTemporality aggregationTemporality = Constants.DEFAULT_AGGREGATION_TEMPORALITY;

  @Override
  public AggregationTemporality getAggregationTemporality() {
    return aggregationTemporality;
  }
}
