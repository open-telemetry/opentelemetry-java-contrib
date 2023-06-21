package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.logs;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.AttributesMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.ByteStringMapper;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = AttributesMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class LogRecordDataMapper {

  public static final LogRecordDataMapper INSTANCE = new LogRecordDataMapperImpl();

  @Mapping(target = "droppedAttributesCount", ignore = true)
  @Mapping(target = "timeUnixNano", source = "timestampEpochNanos")
  @Mapping(target = "observedTimeUnixNano", source = "observedTimestampEpochNanos")
  @Mapping(target = "severityNumber", source = "severity")
  @Mapping(
      target = "flags",
      expression = "java((int) source.getSpanContext().getTraceFlags().asByte())")
  public abstract LogRecord mapToProto(LogRecordData source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = LogRecordDataImpl.class)
  public abstract LogRecordData mapToSdk(
      LogRecord source, @Context Resource resource, @Context InstrumentationScopeInfo scopeInfo);

  @AfterMapping
  protected void addExtrasToProtoBuilder(
      LogRecordData source, @MappingTarget LogRecord.Builder target) {
    target.addAllAttributes(AttributesMapper.INSTANCE.attributesToProto(source.getAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.setSpanId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getSpanId()));
    target.setTraceId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getTraceId()));
    target.setDroppedAttributesCount(
        source.getTotalAttributeCount() - source.getAttributes().size());
  }

  @AfterMapping
  protected void addExtrasToSdkItemBuilder(
      LogRecord source,
      @MappingTarget LogRecordDataImpl.Builder target,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scopeInfo) {
    Attributes attributes = AttributesMapper.INSTANCE.protoToAttributes(source.getAttributesList());
    target.setAttributes(attributes);
    target.setSpanContext(
        SpanContext.create(
            ByteStringMapper.INSTANCE.protoToString(source.getTraceId()),
            ByteStringMapper.INSTANCE.protoToString(source.getSpanId()),
            TraceFlags.getSampled(),
            TraceState.getDefault()));
    target.setTotalAttributeCount(source.getDroppedAttributesCount() + attributes.size());
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scopeInfo);
  }

  protected AnyValue bodyToAnyValue(Body body) {
    return AnyValue.newBuilder().setStringValue(body.asString()).build();
  }

  protected SeverityNumber severityToProto(Severity severity) {
    return SeverityNumber.forNumber(severity.getSeverityNumber());
  }

  protected Body anyValueToBody(AnyValue source) {
    if (source.hasStringValue()) {
      return Body.string(source.getStringValue());
    } else {
      return Body.empty();
    }
  }

  protected Severity severityNumberToSdk(SeverityNumber source) {
    for (Severity value : Severity.values()) {
      if (value.getSeverityNumber() == source.getNumber()) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
