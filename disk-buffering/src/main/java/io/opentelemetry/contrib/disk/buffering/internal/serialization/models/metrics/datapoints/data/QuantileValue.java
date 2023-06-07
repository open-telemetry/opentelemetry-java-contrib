package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data;

import com.dslplatform.json.JsonAttribute;
import javax.annotation.Nullable;

public final class QuantileValue {

  @Nullable
  @JsonAttribute(name = "quantile")
  public Double quantile;

  @Nullable
  @JsonAttribute(name = "value")
  public Double value;
}
