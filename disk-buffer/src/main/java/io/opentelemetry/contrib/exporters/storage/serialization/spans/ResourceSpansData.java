package io.opentelemetry.contrib.exporters.storage.serialization.spans;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.exporters.storage.serialization.common.ResourceSignalsData;
import java.util.Collection;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceSpansData implements ResourceSignalsData<ResourceSpans> {

  @JsonAttribute(name = "resourceSpans")
  public final Collection<ResourceSpans> resourceSpans;

  public ResourceSpansData(Collection<ResourceSpans> resourceSpans) {
    this.resourceSpans = resourceSpans;
  }

  @Override
  public Collection<ResourceSpans> getResourceSignals() {
    return resourceSpans;
  }
}
