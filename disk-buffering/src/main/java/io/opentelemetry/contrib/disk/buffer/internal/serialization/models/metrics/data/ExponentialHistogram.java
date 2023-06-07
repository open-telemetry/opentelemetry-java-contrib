package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.ExponentialHistogramDataPoint;
import javax.annotation.Nullable;

@CompiledJson
public final class ExponentialHistogram extends DataJson<ExponentialHistogramDataPoint> {

  @Nullable
  @JsonAttribute(name = "aggregationTemporality")
  public Integer aggregationTemporality;
}
