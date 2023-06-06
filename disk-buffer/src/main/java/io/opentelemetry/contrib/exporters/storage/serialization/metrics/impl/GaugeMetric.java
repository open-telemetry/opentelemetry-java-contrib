package io.opentelemetry.contrib.exporters.storage.serialization.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.data.DataJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.data.Gauge;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class GaugeMetric extends MetricDataJson {
  public static final String DATA_NAME = "gauge";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Gauge gauge;

  @Override
  public void setData(DataJson<?> data) {
    gauge = (Gauge) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return gauge;
  }
}
