package io.opentelemetry.contrib.exporters.storage.serialization.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import java.util.List;
import javax.annotation.Nullable;

public final class SpanDataJson {
  @Nullable
  @JsonAttribute(name = "traceId")
  public String traceId;

  @Nullable
  @JsonAttribute(name = "spanId")
  public String spanId;

  @Nullable
  @JsonAttribute(name = "parentSpanId")
  public String parentSpanId;

  @Nullable
  @JsonAttribute(name = "name")
  public String name;

  @Nullable
  @JsonAttribute(name = "kind")
  public Integer kind;

  @Nullable
  @JsonAttribute(name = "startTimeUnixNano")
  public String startEpochNanos;

  @Nullable
  @JsonAttribute(name = "endTimeUnixNano")
  public String endEpochNanos;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();

  @Nullable
  @JsonAttribute(name = "droppedAttributesCount")
  public Integer droppedAttributesCount;

  @Nullable
  @JsonAttribute(name = "droppedEventsCount")
  public Integer droppedEventsCount;

  @Nullable
  @JsonAttribute(name = "droppedLinksCount")
  public Integer droppedLinksCount;

  @Nullable
  @JsonAttribute(name = "events")
  public List<EventDataJson> events;

  @Nullable
  @JsonAttribute(name = "links")
  public List<LinkDataJson> links;

  @Nullable
  @JsonAttribute(name = "status")
  public StatusDataJson status;
}
