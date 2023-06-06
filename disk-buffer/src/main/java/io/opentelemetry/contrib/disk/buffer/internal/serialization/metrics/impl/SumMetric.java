package io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Sum;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class SumMetric extends MetricDataJson {
  public static final String DATA_NAME = "sum";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Sum sum;

  @Override
  public void setData(DataJson<?> data) {
    sum = (Sum) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return sum;
  }
}
