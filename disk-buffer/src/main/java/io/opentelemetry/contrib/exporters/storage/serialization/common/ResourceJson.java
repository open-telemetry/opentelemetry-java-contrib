package io.opentelemetry.contrib.exporters.storage.serialization.common;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;

public final class ResourceJson {

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();
}
