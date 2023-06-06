package io.opentelemetry.contrib.disk.buffer.serialization.metrics.datapoints;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.serialization.metrics.datapoints.data.QuantileValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@CompiledJson
public final class SummaryDataPoint extends DataPoint {

  @Nullable
  @JsonAttribute(name = "count")
  public String count;

  @Nullable
  @JsonAttribute(name = "sum")
  public Double sum;

  @JsonAttribute(name = "quantileValues")
  public List<QuantileValue> values = new ArrayList<>();
}
