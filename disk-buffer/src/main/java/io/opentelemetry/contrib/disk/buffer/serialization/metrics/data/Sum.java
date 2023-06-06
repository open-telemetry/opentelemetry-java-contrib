package io.opentelemetry.contrib.disk.buffer.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.serialization.metrics.datapoints.NumberDataPoint;
import javax.annotation.Nullable;

@CompiledJson
public final class Sum extends DataJson<NumberDataPoint> {

  @Nullable
  @JsonAttribute(name = "aggregationTemporality")
  public Integer aggregationTemporality;

  @Nullable
  @JsonAttribute(name = "isMonotonic")
  public Boolean monotonic;
}
