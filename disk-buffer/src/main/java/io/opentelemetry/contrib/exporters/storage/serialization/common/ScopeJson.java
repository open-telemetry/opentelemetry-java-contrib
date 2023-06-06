package io.opentelemetry.contrib.exporters.storage.serialization.common;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class ScopeJson {

  @Nullable
  @JsonAttribute(name = "name")
  public String name;

  @Nullable
  @JsonAttribute(name = "version")
  public String version;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();
}
