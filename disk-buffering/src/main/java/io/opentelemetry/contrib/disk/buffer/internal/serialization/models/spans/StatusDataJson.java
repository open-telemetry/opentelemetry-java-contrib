package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.trace.StatusCode;
import javax.annotation.Nullable;

public final class StatusDataJson {

  @Nullable
  @JsonAttribute(name = "message")
  public String description;

  @JsonAttribute(name = "code")
  public Integer statusCode = StatusCode.UNSET.ordinal();
}
