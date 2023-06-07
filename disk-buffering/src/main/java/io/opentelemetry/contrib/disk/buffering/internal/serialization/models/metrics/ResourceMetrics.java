package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignals;
import java.util.ArrayList;
import java.util.List;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceMetrics extends ResourceSignals<ScopeMetrics> {

  @JsonAttribute(name = "scopeMetrics")
  public List<ScopeMetrics> scopeMetrics = new ArrayList<>();

  @Override
  public void addScopeSignalsItem(ScopeMetrics item) {
    scopeMetrics.add(item);
  }

  @Override
  public List<ScopeMetrics> getScopeSignals() {
    return scopeMetrics;
  }
}
