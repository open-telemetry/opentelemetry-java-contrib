package io.opentelemetry.contrib.exporters.storage.serialization.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class EventDataJson {

  @Nullable
  @JsonAttribute(name = "timeUnixNano")
  public String epochNanos;

  @Nullable
  @JsonAttribute(name = "name")
  public String name;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();

  @Nullable
  @JsonAttribute(name = "droppedAttributesCount")
  public Integer droppedAttributesCount;
}
