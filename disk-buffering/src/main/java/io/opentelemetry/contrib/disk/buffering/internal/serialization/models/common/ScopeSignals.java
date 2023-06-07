package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common;

import com.dslplatform.json.JsonAttribute;
import java.util.List;
import javax.annotation.Nullable;

public abstract class ScopeSignals<T> {

  @Nullable
  @JsonAttribute(name = "scope")
  public ScopeJson scope;

  @Nullable
  @JsonAttribute(name = "schemaUrl")
  public String schemaUrl;

  public abstract void addSignalItem(T item);

  public abstract List<T> getSignalItems();
}
