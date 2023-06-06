package io.opentelemetry.contrib.disk.buffer.internal.serialization.logs;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ResourceSignals;
import java.util.ArrayList;
import java.util.List;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceLogs extends ResourceSignals<ScopeLogs> {

  @JsonAttribute(name = "scopeLogs")
  public List<ScopeLogs> scopeLogs = new ArrayList<>();

  @Override
  public void addScopeSignalsItem(ScopeLogs item) {
    scopeLogs.add(item);
  }

  @Override
  public List<ScopeLogs> getScopeSignals() {
    return scopeLogs;
  }
}
