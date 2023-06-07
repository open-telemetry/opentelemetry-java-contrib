package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.DataPoint;
import java.util.ArrayList;
import java.util.List;

@CompiledJson
public abstract class DataJson<T extends DataPoint> {

  @JsonAttribute(name = "dataPoints")
  public List<T> points = new ArrayList<>();

  @SuppressWarnings("unchecked")
  public void setPoints(List<DataPoint> points) {
    this.points = (List<T>) points;
  }

  @JsonAttribute(ignore = true)
  public List<T> getPoints() {
    return points;
  }
}
