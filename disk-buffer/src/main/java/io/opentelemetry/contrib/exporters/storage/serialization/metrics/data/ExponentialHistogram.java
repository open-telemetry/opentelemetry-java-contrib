package io.opentelemetry.contrib.exporters.storage.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints.ExponentialHistogramDataPoint;
import javax.annotation.Nullable;

@CompiledJson
public final class ExponentialHistogram extends DataJson<ExponentialHistogramDataPoint> {

  @Nullable
  @JsonAttribute(name = "aggregationTemporality")
  public Integer aggregationTemporality;
}
