package io.opentelemetry.contrib.disk.buffer.internal.serialization.logs;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class LogRecordDataJson {

  @Nullable
  @JsonAttribute(name = "timeUnixNano")
  public String epochNanos;

  @Nullable
  @JsonAttribute(name = "severityNumber")
  public Integer severity;

  @Nullable
  @JsonAttribute(name = "severityText")
  public String severityText;

  @Nullable
  @JsonAttribute(name = "body")
  public BodyJson body;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();

  @Nullable
  @JsonAttribute(name = "droppedAttributesCount")
  public Integer droppedAttributesCount;

  @Nullable
  @JsonAttribute(name = "flags")
  public Integer flags;

  @Nullable
  @JsonAttribute(name = "traceId")
  public String traceId;

  @Nullable
  @JsonAttribute(name = "spanId")
  public String spanId;
}
