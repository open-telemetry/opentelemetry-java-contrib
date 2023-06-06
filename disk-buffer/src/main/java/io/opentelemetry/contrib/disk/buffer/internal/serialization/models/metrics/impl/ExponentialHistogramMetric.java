package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.ExponentialHistogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.MetricDataJson;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ExponentialHistogramMetric extends MetricDataJson {
  public static final String DATA_NAME = "exponentialHistogram";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public ExponentialHistogram exponentialHistogram;

  @Override
  public void setData(DataJson<?> data) {
    exponentialHistogram = (ExponentialHistogram) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return exponentialHistogram;
  }
}
