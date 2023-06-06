package io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public abstract class DataPoint {

  @Nullable
  @JsonAttribute(name = "startTimeUnixNano")
  public String startEpochNanos;

  @Nullable
  @JsonAttribute(name = "timeUnixNano")
  public String epochNanos;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();
}
