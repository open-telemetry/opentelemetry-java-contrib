package io.opentelemetry.contrib.exporters.storage.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints.HistogramDataPoint;
import javax.annotation.Nullable;

@CompiledJson
public final class Histogram extends DataJson<HistogramDataPoint> {

  @Nullable
  @JsonAttribute(name = "aggregationTemporality")
  public Integer aggregationTemporality;
}
