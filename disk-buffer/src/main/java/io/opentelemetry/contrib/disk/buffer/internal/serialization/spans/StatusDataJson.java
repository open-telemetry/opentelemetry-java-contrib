package io.opentelemetry.contrib.disk.buffer.internal.serialization.spans;

import com.dslplatform.json.JsonAttribute;
import javax.annotation.Nullable;

public final class StatusDataJson {

  @Nullable
  @JsonAttribute(name = "message")
  public String description;

  @Nullable
  @JsonAttribute(name = "code")
  public Integer statusCode;
}
