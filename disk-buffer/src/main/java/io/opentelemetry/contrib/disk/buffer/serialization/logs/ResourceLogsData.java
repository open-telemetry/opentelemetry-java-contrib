package io.opentelemetry.contrib.disk.buffer.serialization.logs;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.serialization.common.ResourceSignalsData;
import java.util.Collection;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceLogsData implements ResourceSignalsData<ResourceLogs> {

  @JsonAttribute(name = "resourceLogs")
  public final Collection<ResourceLogs> resourceLogs;

  public ResourceLogsData(Collection<ResourceLogs> resourceLogs) {
    this.resourceLogs = resourceLogs;
  }

  @Override
  public Collection<ResourceLogs> getResourceSignals() {
    return resourceLogs;
  }
}
