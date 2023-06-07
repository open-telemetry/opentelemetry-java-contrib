package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import java.util.List;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public abstract class ResourceSignals<T extends ScopeSignals<?>> {

  @Nullable
  @JsonAttribute(name = "resource")
  public ResourceJson resource;

  @Nullable
  @JsonAttribute(name = "schemaUrl")
  public String schemaUrl;

  public abstract void addScopeSignalsItem(T item);

  public abstract List<T> getScopeSignals();
}
