package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;

public abstract class GaugeDataImpl<T extends PointData> extends DataImpl<T>
    implements GaugeData<T> {

  public static class LongData extends GaugeDataImpl<LongPointData> {}

  public static class DoubleData extends GaugeDataImpl<DoublePointData> {}
}
