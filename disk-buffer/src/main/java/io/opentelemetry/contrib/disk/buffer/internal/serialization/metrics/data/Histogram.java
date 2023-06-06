package io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.datapoints.HistogramDataPoint;
import javax.annotation.Nullable;

@CompiledJson
public final class Histogram extends DataJson<HistogramDataPoint> {

  @Nullable
  @JsonAttribute(name = "aggregationTemporality")
  public Integer aggregationTemporality;
}
