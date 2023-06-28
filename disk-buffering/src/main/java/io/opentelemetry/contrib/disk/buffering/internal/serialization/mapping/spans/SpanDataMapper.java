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
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.data.EventDataImpl;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
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
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Context;
import org.mapstruct.EnumMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

@Mapper(
    uses = ByteStringMapper.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class SpanDataMapper {

  public static final SpanDataMapper INSTANCE = new SpanDataMapperImpl();

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "endTimeUnixNano", source = "endEpochNanos")
  @Mapping(target = "eventsList", source = "events")
  @Mapping(target = "linksList", source = "links")
  public abstract Span mapToProto(SpanData source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = SpanDataImpl.class)
  public abstract SpanData mapToSdk(
      Span source,
      @Context Resource resource,
      @Context InstrumentationScopeInfo instrumentationScopeInfo);

  @Mapping(target = "timeUnixNano", source = "epochNanos")
  protected abstract Span.Event eventDataToProto(EventData source);

  protected Span.Link linkDataToProto(LinkData source) {
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

  @Mapping(target = "message", source = "description")
  @Mapping(target = "code", source = "statusCode")
  protected abstract Status statusDataToProto(StatusData source);

  @AfterMapping
  protected void addSpanProtoExtras(SpanData source, @MappingTarget Span.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
    target.setDroppedAttributesCount(
        source.getTotalAttributeCount() - source.getAttributes().size());
    target.setDroppedEventsCount(source.getTotalRecordedEvents() - getListSize(source.getEvents()));
    target.setDroppedLinksCount(source.getTotalRecordedLinks() - getListSize(source.getLinks()));
    target.setTraceState(encodeTraceState(source.getSpanContext().getTraceState()));
  }

  @AfterMapping
  protected void addEventProtoExtras(EventData source, @MappingTarget Span.Event.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  @EnumMapping(
      nameTransformationStrategy = MappingConstants.PREFIX_TRANSFORMATION,
      configuration = "SPAN_KIND_")
  protected abstract Span.SpanKind mapSpanKindToProto(SpanKind source);

  @EnumMapping(
      nameTransformationStrategy = MappingConstants.PREFIX_TRANSFORMATION,
      configuration = "STATUS_CODE_")
  protected abstract Status.StatusCode mapStatusCodeToProto(StatusCode source);

  // FROM PROTO
  protected StatusData mapStatusDataToSdk(Status source) {
    return StatusData.create(getStatusCode(source.getCodeValue()), source.getMessage());
  }

  @InheritInverseConfiguration
  @BeanMapping(resultType = EventDataImpl.class)
  protected abstract EventData eventDataToSdk(Span.Event source);

  protected LinkData linkDataToSdk(Span.Link source) {
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

  @AfterMapping
  protected void addEventSdkExtras(Span.Event source, @MappingTarget EventDataImpl.Builder target) {
    Attributes attributes = protoToAttributes(source.getAttributesList());
    target.setAttributes(attributes);
    target.setTotalAttributeCount(attributes.size() + source.getDroppedAttributesCount());
  }

  @AfterMapping
  protected void addSpanDataExtras(
      Span source,
      @MappingTarget SpanDataImpl.Builder target,
      @Context Resource resource,
      @Context InstrumentationScopeInfo instrumentationScopeInfo) {
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

  @EnumMapping(
      nameTransformationStrategy = MappingConstants.STRIP_PREFIX_TRANSFORMATION,
      configuration = "SPAN_KIND_")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
  protected abstract SpanKind mapSpanKindToSdk(Span.SpanKind source);

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
}
