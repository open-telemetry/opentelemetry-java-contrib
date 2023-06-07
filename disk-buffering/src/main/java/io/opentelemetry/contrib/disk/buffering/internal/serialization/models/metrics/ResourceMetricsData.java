package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignalsData;
import java.util.Collection;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceMetricsData implements ResourceSignalsData<ResourceMetrics> {

  @JsonAttribute(name = "resourceMetrics")
  public final Collection<ResourceMetrics> resourceMetrics;

  public ResourceMetricsData(Collection<ResourceMetrics> resourceMetrics) {
    this.resourceMetrics = resourceMetrics;
  }

  @Override
  public Collection<ResourceMetrics> getResourceSignals() {
    return resourceMetrics;
  }
}
