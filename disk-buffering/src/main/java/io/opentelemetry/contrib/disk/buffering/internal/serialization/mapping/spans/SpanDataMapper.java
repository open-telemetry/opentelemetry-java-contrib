/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import static io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools.toUnsignedInt;

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

public class SpanDataMapper {

  private static final SpanDataMapper INSTANCE = new SpanDataMapper();

  public static SpanDataMapper getInstance() {
    return INSTANCE;
  }

  private final ByteStringMapper byteStringMapper = ByteStringMapper.getInstance();

  public Span mapToProto(SpanData source) {
    Span.Builder span = new Span.Builder();

    span.start_time_unix_nano(source.getStartEpochNanos());
    span.end_time_unix_nano(source.getEndEpochNanos());
    if (source.getEvents() != null) {
      for (EventData event : source.getEvents()) {
        span.events.add(eventDataToProto(event));
      }
    }
    if (source.getLinks() != null) {
      for (LinkData link : source.getLinks()) {
        span.links.add(linkDataToProto(link));
      }
    }
    span.trace_id(byteStringMapper.stringToProto(source.getTraceId()));
    span.span_id(byteStringMapper.stringToProto(source.getSpanId()));
    span.flags(source.getSpanContext().getTraceFlags().asByte());
    span.parent_span_id(byteStringMapper.stringToProto(source.getParentSpanId()));
    span.name(source.getName());
    span.kind(mapSpanKindToProto(source.getKind()));
    span.status(statusDataToProto(source.getStatus()));

    addSpanProtoExtras(source, span);

    return span.build();
  }

  private static void addSpanProtoExtras(SpanData source, Span.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
    target.dropped_attributes_count(
        source.getTotalAttributeCount() - source.getAttributes().size());
    target.dropped_events_count(source.getTotalRecordedEvents() - getListSize(source.getEvents()));
    target.dropped_links_count(source.getTotalRecordedLinks() - getListSize(source.getLinks()));
    target.trace_state(encodeTraceState(source.getSpanContext().getTraceState()));
  }

  public SpanData mapToSdk(
      Span source, Resource resource, InstrumentationScopeInfo instrumentationScopeInfo) {
    SpanDataImpl.Builder spanData = SpanDataImpl.builder();

    spanData.setStartEpochNanos(source.start_time_unix_nano);
    spanData.setEndEpochNanos(source.end_time_unix_nano);
    spanData.setEvents(eventListToEventDataList(source.events));
    spanData.setLinks(linkListToLinkDataList(source.links));
    spanData.setName(source.name);
    spanData.setKind(mapSpanKindToSdk(source.kind));
    if (source.status != null) {
      spanData.setStatus(mapStatusDataToSdk(source.status));
    }

    addSpanDataExtras(source, spanData, resource, instrumentationScopeInfo);

    return spanData.build();
  }

  private static void addSpanDataExtras(
      Span source,
      SpanDataImpl.Builder target,
      Resource resource,
      InstrumentationScopeInfo instrumentationScopeInfo) {
    Attributes attributes = protoToAttributes(source.attributes);
    target.setAttributes(attributes);
    target.setResource(resource);
    target.setInstrumentationScopeInfo(instrumentationScopeInfo);
    String traceId = ByteStringMapper.getInstance().protoToString(source.trace_id);
    target.setSpanContext(
        SpanContext.create(
            traceId,
            ByteStringMapper.getInstance().protoToString(source.span_id),
            flagsFromInt(source.flags),
            decodeTraceState(source.trace_state)));
    target.setParentSpanContext(
        SpanContext.create(
            traceId,
            ByteStringMapper.getInstance().protoToString(source.parent_span_id),
            TraceFlags.getSampled(),
            TraceState.getDefault()));
    target.setTotalAttributeCount(source.dropped_attributes_count + attributes.size());
    target.setTotalRecordedEvents(
        calculateRecordedItems(source.dropped_events_count, source.events.size()));
    target.setTotalRecordedLinks(
        calculateRecordedItems(source.dropped_links_count, source.links.size()));
  }

  private static StatusData mapStatusDataToSdk(Status source) {
    return StatusData.create(getStatusCode(source.code), source.message);
  }

  private static Span.Event eventDataToProto(EventData source) {
    Span.Event.Builder event = new Span.Event.Builder();

    event.time_unix_nano(source.getEpochNanos());
    event.name(source.getName());
    event.dropped_attributes_count(source.getDroppedAttributesCount());

    addEventProtoExtras(source, event);

    return event.build();
  }

  private static void addEventProtoExtras(EventData source, Span.Event.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
  }

  private static Status statusDataToProto(StatusData source) {
    Status.Builder status = new Status.Builder();

    status.message(source.getDescription());
    status.code(mapStatusCodeToProto(source.getStatusCode()));

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
    Attributes attributes = protoToAttributes(source.attributes);
    return EventData.create(
        source.time_unix_nano,
        source.name,
        attributes,
        attributes.size() + source.dropped_attributes_count);
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
    Attributes attributes = protoToAttributes(source.attributes);
    int totalAttrCount = source.dropped_attributes_count + attributes.size();
    SpanContext spanContext =
        SpanContext.create(
            ByteStringMapper.getInstance().protoToString(source.trace_id),
            ByteStringMapper.getInstance().protoToString(source.span_id),
            flagsFromInt(source.flags),
            decodeTraceState(source.trace_state));
    return LinkData.create(spanContext, attributes, totalAttrCount);
  }

  private static int calculateRecordedItems(int droppedCount, int itemsCount) {
    return droppedCount + itemsCount;
  }

  private static StatusCode getStatusCode(Status.StatusCode source) {
    switch (source) {
      case STATUS_CODE_UNSET:
        return StatusCode.UNSET;
      case STATUS_CODE_OK:
        return StatusCode.OK;
      case STATUS_CODE_ERROR:
        return StatusCode.ERROR;
    }
    throw new IllegalArgumentException("Unexpected enum constant: " + source);
  }

  private static List<KeyValue> attributesToProto(Attributes source) {
    return AttributesMapper.getInstance().attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.getInstance().protoToAttributes(source);
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
    Span.Link.Builder builder = new Span.Link.Builder();
    SpanContext spanContext = source.getSpanContext();
    builder.trace_id(ByteStringMapper.getInstance().stringToProto(spanContext.getTraceId()));
    builder.span_id(ByteStringMapper.getInstance().stringToProto(spanContext.getSpanId()));
    builder.flags = toUnsignedInt(spanContext.getTraceFlags().asByte());
    builder.attributes.addAll(attributesToProto(source.getAttributes()));
    builder.dropped_attributes_count(
        source.getTotalAttributeCount() - source.getAttributes().size());
    builder.trace_state(encodeTraceState(spanContext.getTraceState()));

    return builder.build();
  }

  public static TraceFlags flagsFromInt(int b) {
    return TraceFlags.fromByte((byte) (b & 0x000000FF));
  }
}
