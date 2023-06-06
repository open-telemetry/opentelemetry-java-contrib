package io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints.data.Exemplar;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class NumberDataPoint extends DataPoint {

  @Nullable
  @JsonAttribute(name = "asInt")
  public String longValue;

  @Nullable
  @JsonAttribute(name = "asDouble")
  public Double doubleValue;

  @JsonAttribute(name = "exemplars")
  public List<Exemplar> exemplars = new ArrayList<>();

  public void setValue(Object value) {
    if (value instanceof Long) {
      longValue = String.valueOf(value);
    } else {
      doubleValue = (Double) value;
    }
  }

  public Type getType() {
    if (longValue != null) {
      return Type.LONG;
    } else {
      return Type.DOUBLE;
    }
  }

  public enum Type {
    LONG,
    DOUBLE
  }
}
