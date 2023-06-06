package io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ScopeSignals;
import java.util.ArrayList;
import java.util.List;

public final class ScopeMetrics extends ScopeSignals<MetricDataJson> {

  @JsonAttribute(name = "metrics")
  public List<MetricDataJson> metrics = new ArrayList<>();

  @Override
  public void addSignalItem(MetricDataJson item) {
    metrics.add(item);
  }

  @Override
  public List<MetricDataJson> getSignalItems() {
    return metrics;
  }
}
