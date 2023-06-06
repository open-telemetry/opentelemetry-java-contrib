package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.common.SpanContextMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.data.EventDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.data.LinkDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.spans.EventDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.spans.LinkDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.spans.SpanDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.spans.StatusDataJson;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import javax.annotation.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public abstract class SpanDataMapper {

  public static final SpanDataMapper INSTANCE = Mappers.getMapper(SpanDataMapper.class);

  @SpanContextMapping
  @Mapping(target = "droppedAttributesCount", ignore = true)
  @Mapping(target = "droppedEventsCount", ignore = true)
  @Mapping(target = "droppedLinksCount", ignore = true)
  public abstract SpanDataJson spanDataToJson(SpanData source);

  @SpanContextMapping
  @Mapping(target = "droppedAttributesCount", ignore = true)
  protected abstract LinkDataJson linkDataToJson(LinkData source);

  @AfterMapping
  protected void addDroppedCount(SpanData source, @MappingTarget SpanDataJson target) {
    target.droppedAttributesCount = source.getTotalAttributeCount() - source.getAttributes().size();
    target.droppedEventsCount = source.getTotalRecordedEvents() - getListSize(source.getEvents());
    target.droppedLinksCount = source.getTotalRecordedLinks() - getListSize(source.getLinks());
  }

  private static int getListSize(List<?> list) {
    if (list == null) {
      return 0;
    }
    return list.size();
  }

  @AfterMapping
  protected void addDroppedAttributesCount(LinkData source, @MappingTarget LinkDataJson target) {
    target.droppedAttributesCount = source.getTotalAttributeCount() - source.getAttributes().size();
  }

  protected Integer mapSpanKindToJson(SpanKind value) {
    return value.ordinal() + 1;
  }

  protected Integer mapStatusCodeToJson(StatusCode value) {
    return value.ordinal();
  }

  // FROM JSON
  @BeanMapping(resultType = SpanDataImpl.class)
  @Mapping(target = "spanContext", ignore = true)
  @Mapping(target = "parentSpanContext", ignore = true)
  @Mapping(target = "totalAttributeCount", ignore = true)
  @Mapping(target = "totalRecordedEvents", ignore = true)
  @Mapping(target = "totalRecordedLinks", ignore = true)
  @Mapping(target = "instrumentationScopeInfo", ignore = true)
  @Mapping(target = "resource", ignore = true)
  public abstract SpanData jsonToSpanData(
      SpanDataJson source,
      @Context Resource resource,
      @Context InstrumentationScopeInfo instrumentationScopeInfo);

  @BeanMapping(resultType = LinkDataImpl.class)
  @Mapping(target = "spanContext", ignore = true)
  @Mapping(target = "totalAttributeCount", ignore = true)
  protected abstract LinkData jsonToLinkData(LinkDataJson source);

  @BeanMapping(resultType = EventDataImpl.class)
  @Mapping(target = "totalAttributeCount", ignore = true)
  protected abstract EventData jsonToEventData(EventDataJson source);

  @AfterMapping
  protected void addSpanDataJsonExtras(
      SpanDataJson source,
      @MappingTarget SpanDataImpl.Builder target,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scopeInfo) {
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scopeInfo);
    if (source.traceId != null) {
      if (source.spanId != null) {
        target.setSpanContext(
            SpanContext.create(
                source.traceId, source.spanId, TraceFlags.getSampled(), TraceState.getDefault()));
      }
      if (source.parentSpanId != null) {
        target.setParentSpanContext(
            SpanContext.create(
                source.traceId,
                source.parentSpanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()));
      }
    }
    target.setTotalAttributeCount(source.droppedAttributesCount + source.attributes.size());
    target.setTotalRecordedEvents(calculateRecordedItems(source.droppedEventsCount, source.events));
    target.setTotalRecordedLinks(calculateRecordedItems(source.droppedLinksCount, source.links));
  }

  private static int calculateRecordedItems(int droppedCount, @Nullable List<?> items) {
    if (items == null) {
      return 0;
    }
    return droppedCount + items.size();
  }

  @AfterMapping
  protected void addTotalAttributesCount(
      EventDataJson source, @MappingTarget EventDataImpl.Builder target) {
    target.setTotalAttributeCount(source.droppedAttributesCount + source.attributes.size());
  }

  @AfterMapping
  protected void addLinkDataJsonExtras(
      LinkDataJson source, @MappingTarget LinkDataImpl.Builder target) {
    target.setTotalAttributeCount(source.droppedAttributesCount + source.attributes.size());
    if (source.traceId != null && source.spanId != null) {
      target.setSpanContext(
          SpanContext.create(
              source.traceId, source.spanId, TraceFlags.getSampled(), TraceState.getDefault()));
    }
  }

  protected StatusData jsonToStatusData(StatusDataJson source) {
    return StatusData.create(getStatusCode(source.statusCode), source.description);
  }

  private static StatusCode getStatusCode(int ordinal) {
    for (StatusCode statusCode : StatusCode.values()) {
      if (statusCode.ordinal() == ordinal) {
        return statusCode;
      }
    }
    throw new IllegalArgumentException();
  }

  protected SpanKind jsonToSpanKind(Integer value) {
    int ordinal = value - 1;
    for (SpanKind spanKind : SpanKind.values()) {
      if (spanKind.ordinal() == ordinal) {
        return spanKind;
      }
    }
    throw new IllegalArgumentException();
  }
}
