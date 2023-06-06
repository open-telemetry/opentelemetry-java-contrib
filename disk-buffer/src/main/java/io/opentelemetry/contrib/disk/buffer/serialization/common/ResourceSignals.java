package io.opentelemetry.contrib.disk.buffer.serialization.common;

import com.dslplatform.json.JsonAttribute;
import java.util.List;
import javax.annotation.Nullable;

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
