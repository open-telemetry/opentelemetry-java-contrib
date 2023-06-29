/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.AttributesMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ByteStringMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public final class SpanDataMapper {

  public static final SpanDataMapper INSTANCE = new SpanDataMapper();
  private final ByteStringMapper byteStringMapper = ByteStringMapper.INSTANCE;

  public Span mapToProto(SpanData source) {
    Span.Builder span = Span.newBuilder();

    span.setStartTimeUnixNano(source.getStartEpochNanos());
    span.setEndTimeUnixNano(source.getEndEpochNanos());
    if (source.getEvents() != null) {
      for (EventData event : source.getEvents()) {
        span.addEvents(eventDataToProto(event));
      }
    }
    if (source.getLinks() != null) {
      for (LinkData link : source.getLinks()) {
        span.addLinks(linkDataToProto(link));
      }
    }
    span.setTraceId(byteStringMapper.stringToProto(source.getTraceId()));
    span.setSpanId(byteStringMapper.stringToProto(source.getSpanId()));
    span.setParentSpanId(byteStringMapper.stringToProto(source.getParentSpanId()));
    span.setName(source.getName());
    span.setKind(mapSpanKindToProto(source.getKind()));
    span.setStatus(statusDataToProto(source.getStatus()));

    addSpanProtoExtras(source, span);

    return span.build();
  }

  private static void addSpanProtoExtras(SpanData source, Span.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
    target.setDroppedAttributesCount(
        source.getTotalAttributeCount() - source.getAttributes().size());
    target.setDroppedEventsCount(source.getTotalRecordedEvents() - getListSize(source.getEvents()));
    target.setDroppedLinksCount(source.getTotalRecordedLinks() - getListSize(source.getLinks()));
    target.setTraceState(encodeTraceState(source.getSpanContext().getTraceState()));
  }

  public SpanData mapToSdk(
      Span source, Resource resource, InstrumentationScopeInfo instrumentationScopeInfo) {
    SpanDataImpl.Builder spanData = SpanDataImpl.builder();

    spanData.setStartEpochNanos(source.getStartTimeUnixNano());
    spanData.setEndEpochNanos(source.getEndTimeUnixNano());
    spanData.setEvents(eventListToEventDataList(source.getEventsList()));
    spanData.setLinks(linkListToLinkDataList(source.getLinksList()));
    spanData.setName(source.getName());
    spanData.setKind(mapSpanKindToSdk(source.getKind()));
    if (source.hasStatus()) {
      spanData.setStatus(mapStatusDataToSdk(source.getStatus()));
    }

    addSpanDataExtras(source, spanData, resource, instrumentationScopeInfo);

    return spanData.build();
  }

  private static void addSpanDataExtras(
      Span source,
      SpanDataImpl.Builder target,
      Resource resource,
      InstrumentationScopeInfo instrumentationScopeInfo) {
    Attributes attributes = protoToAttributes(source.getAttributesList());
    target.setAttributes(attributes);
    target.setResource(resource);
    target.setInstrumentationScopeInfo(instrumentationScopeInfo);
    String traceId = ByteStringMapper.INSTANCE.protoToString(source.getTraceId());
    target.setSpanContext(
        SpanContext.create(
            traceId,
            ByteStringMapper.INSTANCE.protoToString(source.getSpanId()),
            TraceFlags.getSampled(),
            decodeTraceState(source.getTraceState())));
    target.setParentSpanContext(
        SpanContext.create(
            traceId,
            ByteStringMapper.INSTANCE.protoToString(source.getParentSpanId()),
            TraceFlags.getSampled(),
            TraceState.getDefault()));
    target.setTotalAttributeCount(source.getDroppedAttributesCount() + attributes.size());
    target.setTotalRecordedEvents(
        calculateRecordedItems(source.getDroppedEventsCount(), source.getEventsCount()));
    target.setTotalRecordedLinks(
        calculateRecordedItems(source.getDroppedLinksCount(), source.getLinksCount()));
  }

  private static StatusData mapStatusDataToSdk(Status source) {
    return StatusData.create(getStatusCode(source.getCodeValue()), source.getMessage());
  }

  private static Span.Event eventDataToProto(EventData source) {
    Span.Event.Builder event = Span.Event.newBuilder();

    event.setTimeUnixNano(source.getEpochNanos());
    event.setName(source.getName());
    event.setDroppedAttributesCount(source.getDroppedAttributesCount());

    addEventProtoExtras(source, event);

    return event.build();
  }

  private static void addEventProtoExtras(EventData source, Span.Event.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  private static Status statusDataToProto(StatusData source) {
    Status.Builder status = Status.newBuilder();

    status.setMessage(source.getDescription());
    status.setCode(mapStatusCodeToProto(source.getStatusCode()));

    return status.build();
  }

  private static Span.SpanKind mapSpanKindToProto(SpanKind source) {
    Span.SpanKind spanKind;

    switch (source) {
      case INTERNAL:
        spanKind = Span.SpanKind.SPAN_KIND_INTERNAL;
        break;
      case SERVER:
        spanKind = Span.SpanKind.SPAN_KIND_SERVER;
        break;
      case CLIENT:
        spanKind = Span.SpanKind.SPAN_KIND_CLIENT;
        break;
      case PRODUCER:
        spanKind = Span.SpanKind.SPAN_KIND_PRODUCER;
        break;
      case CONSUMER:
        spanKind = Span.SpanKind.SPAN_KIND_CONSUMER;
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum constant: " + source);
    }

    return spanKind;
  }

  private static Status.StatusCode mapStatusCodeToProto(StatusCode source) {
    Status.StatusCode statusCode;

    switch (source) {
      case UNSET:
        statusCode = Status.StatusCode.STATUS_CODE_UNSET;
        break;
      case OK:
        statusCode = Status.StatusCode.STATUS_CODE_OK;
        break;
      case ERROR:
        statusCode = Status.StatusCode.STATUS_CODE_ERROR;
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum constant: " + source);
    }

    return statusCode;
  }

  private static EventData eventDataToSdk(Span.Event source) {
    Attributes attributes = protoToAttributes(source.getAttributesList());
    return EventData.create(
        source.getTimeUnixNano(),
        source.getName(),
        attributes,
        attributes.size() + source.getDroppedAttributesCount());
  }

  private static SpanKind mapSpanKindToSdk(Span.SpanKind source) {
    SpanKind spanKind;

    switch (source) {
      case SPAN_KIND_INTERNAL:
        spanKind = SpanKind.INTERNAL;
        break;
      case SPAN_KIND_SERVER:
        spanKind = SpanKind.SERVER;
        break;
      case SPAN_KIND_CLIENT:
        spanKind = SpanKind.CLIENT;
        break;
      case SPAN_KIND_PRODUCER:
        spanKind = SpanKind.PRODUCER;
        break;
      case SPAN_KIND_CONSUMER:
        spanKind = SpanKind.CONSUMER;
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum constant: " + source);
    }

    return spanKind;
  }

  private static List<EventData> eventListToEventDataList(List<Span.Event> list) {
    List<EventData> result = new ArrayList<>(list.size());
    for (Span.Event event : list) {
      result.add(eventDataToSdk(event));
    }

    return result;
  }

  private static List<LinkData> linkListToLinkDataList(List<Span.Link> list) {
    List<LinkData> result = new ArrayList<>(list.size());
    for (Span.Link link : list) {
      result.add(linkDataToSdk(link));
    }

    return result;
  }

  private static LinkData linkDataToSdk(Span.Link source) {
    Attributes attributes = protoToAttributes(source.getAttributesList());
    int totalAttrCount = source.getDroppedAttributesCount() + attributes.size();
    SpanContext spanContext =
        SpanContext.create(
            ByteStringMapper.INSTANCE.protoToString(source.getTraceId()),
            ByteStringMapper.INSTANCE.protoToString(source.getSpanId()),
            TraceFlags.getSampled(),
            decodeTraceState(source.getTraceState()));
    return LinkData.create(spanContext, attributes, totalAttrCount);
  }

  private static int calculateRecordedItems(int droppedCount, int itemsCount) {
    return droppedCount + itemsCount;
  }

  private static StatusCode getStatusCode(int ordinal) {
    for (StatusCode statusCode : StatusCode.values()) {
      if (statusCode.ordinal() == ordinal) {
        return statusCode;
      }
    }
    throw new IllegalArgumentException();
  }

  private static List<KeyValue> attributesToProto(Attributes source) {
    return AttributesMapper.INSTANCE.attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.INSTANCE.protoToAttributes(source);
  }

  private static int getListSize(List<?> list) {
    if (list == null) {
      return 0;
    }
    return list.size();
  }

  private static String encodeTraceState(TraceState traceState) {
    if (!traceState.isEmpty()) {
      return W3CTraceContextEncoding.encodeTraceState(traceState);
    }
    return "";
  }

  private static TraceState decodeTraceState(@Nullable String source) {
    return (source == null || source.isEmpty())
        ? TraceState.getDefault()
        : W3CTraceContextEncoding.decodeTraceState(source);
  }

  private static Span.Link linkDataToProto(LinkData source) {
    Span.Link.Builder builder = Span.Link.newBuilder();
    SpanContext spanContext = source.getSpanContext();
    builder.setTraceId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getTraceId()));
    builder.setSpanId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getSpanId()));
    builder.addAllAttributes(attributesToProto(source.getAttributes()));
    builder.setDroppedAttributesCount(
        source.getTotalAttributeCount() - source.getAttributes().size());
    builder.setTraceState(encodeTraceState(spanContext.getTraceState()));

    return builder.build();
  }
}
