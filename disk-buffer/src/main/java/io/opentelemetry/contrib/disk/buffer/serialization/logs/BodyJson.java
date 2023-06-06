package io.opentelemetry.contrib.disk.buffer.serialization.logs;

import com.dslplatform.json.JsonAttribute;
import javax.annotation.Nullable;

public final class BodyJson {

  @Nullable
  @JsonAttribute(name = "stringValue")
  public String stringValue;
}
