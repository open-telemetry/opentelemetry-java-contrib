package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.logs;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.common.SpanContextMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.logs.BodyJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.logs.LogRecordDataJson;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public abstract class LogRecordMapper {

  public static final LogRecordMapper INSTANCE = Mappers.getMapper(LogRecordMapper.class);

  @SpanContextMapping
  @Mapping(
      target = "flags",
      expression = "java((int) source.getSpanContext().getTraceFlags().asByte())")
  @Mapping(target = "droppedAttributesCount", ignore = true)
  public abstract LogRecordDataJson logToJson(LogRecordData source);

  protected BodyJson bodyToJson(Body source) {
    BodyJson bodyJson = new BodyJson();
    bodyJson.stringValue = source.asString();
    return bodyJson;
  }

  protected Integer severityToNumber(Severity severity) {
    return severity.getSeverityNumber();
  }

  @AfterMapping
  protected void addDroppedAttributesCount(
      LogRecordData source, @MappingTarget LogRecordDataJson target) {
    target.droppedAttributesCount = source.getTotalAttributeCount() - source.getAttributes().size();
  }

  // FROM JSON
  @BeanMapping(resultType = LogRecordDataImpl.class)
  @Mapping(target = "resource", ignore = true)
  @Mapping(target = "instrumentationScopeInfo", ignore = true)
  @Mapping(target = "spanContext", ignore = true)
  @Mapping(target = "totalAttributeCount", ignore = true)
  public abstract LogRecordData jsonToLog(
      LogRecordDataJson source,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scopeInfo);

  @AfterMapping
  protected void addExtras(
      LogRecordDataJson source,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scopeInfo,
      @MappingTarget LogRecordDataImpl.Builder target) {
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scopeInfo);
    if (source.traceId != null && source.spanId != null) {
      target.setSpanContext(
          SpanContext.create(
              source.traceId, source.spanId, TraceFlags.getSampled(), TraceState.getDefault()));
    }
    target.setTotalAttributeCount(source.droppedAttributesCount + source.attributes.size());
  }

  protected Body jsonToBody(BodyJson source) {
    if (source.stringValue != null) {
      return Body.string(source.stringValue);
    } else {
      return Body.empty();
    }
  }

  protected Severity jsonToSeverity(Integer source) {
    for (Severity severity : Severity.values()) {
      if (severity.getSeverityNumber() == source) {
        return severity;
      }
    }
    throw new IllegalArgumentException();
  }
}
