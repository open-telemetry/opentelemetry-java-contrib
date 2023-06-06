package io.opentelemetry.contrib.disk.buffer.serialization.spans;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.serialization.common.ResourceSignals;
import java.util.ArrayList;
import java.util.List;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceSpans extends ResourceSignals<ScopeSpan> {

  @JsonAttribute(name = "scopeSpans")
  public List<ScopeSpan> scopeSpans = new ArrayList<>();

  @Override
  public void addScopeSignalsItem(ScopeSpan item) {
    scopeSpans.add(item);
  }

  @Override
  public List<ScopeSpan> getScopeSignals() {
    return scopeSpans;
  }
}
