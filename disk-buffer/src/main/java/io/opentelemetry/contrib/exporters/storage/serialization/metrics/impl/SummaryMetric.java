package io.opentelemetry.contrib.exporters.storage.serialization.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.data.DataJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.data.Summary;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class SummaryMetric extends MetricDataJson {
  public static final String DATA_NAME = "summary";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Summary summary;

  @Override
  public void setData(DataJson<?> data) {
    summary = (Summary) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return summary;
  }
}
