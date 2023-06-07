package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class Exemplar {

  @Nullable
  @JsonAttribute(name = "timeUnixNano")
  public String epochNanos;

  @Nullable
  @JsonAttribute(name = "spanId")
  public String spanId;

  @Nullable
  @JsonAttribute(name = "traceId")
  public String traceId;

  @JsonAttribute(name = "filteredAttributes")
  public Attributes filteredAttributes = Attributes.empty();

  @Nullable
  @JsonAttribute(name = "asDouble")
  public Double doubleValue;

  @Nullable
  @JsonAttribute(name = "asInt")
  public String longValue;

  public void setValue(Object value) {
    if (value instanceof Long) {
      longValue = String.valueOf(value);
    } else {
      doubleValue = (Double) value;
    }
  }
}
