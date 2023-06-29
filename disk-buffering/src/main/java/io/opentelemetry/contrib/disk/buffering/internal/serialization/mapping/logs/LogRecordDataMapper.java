/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.AttributesMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ByteStringMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;

public final class LogRecordDataMapper {

  public static final LogRecordDataMapper INSTANCE = new LogRecordDataMapper();

  public LogRecord mapToProto(LogRecordData source) {
    LogRecord.Builder logRecord = LogRecord.newBuilder();

    logRecord.setTimeUnixNano(source.getTimestampEpochNanos());
    logRecord.setObservedTimeUnixNano(source.getObservedTimestampEpochNanos());
    if (source.getSeverity() != null) {
      logRecord.setSeverityNumber(severityToProto(source.getSeverity()));
    }
    if (source.getSeverityText() != null) {
      logRecord.setSeverityText(source.getSeverityText());
    }
    if (source.getBody() != null) {
      logRecord.setBody(bodyToAnyValue(source.getBody()));
    }

    logRecord.setFlags(source.getSpanContext().getTraceFlags().asByte());

    addExtrasToProtoBuilder(source, logRecord);

    return logRecord.build();
  }

  private static void addExtrasToProtoBuilder(LogRecordData source, LogRecord.Builder target) {
    target.addAllAttributes(AttributesMapper.INSTANCE.attributesToProto(source.getAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.setSpanId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getSpanId()));
    target.setTraceId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getTraceId()));
    target.setDroppedAttributesCount(
        source.getTotalAttributeCount() - source.getAttributes().size());
  }

  public LogRecordData mapToSdk(
      LogRecord source, Resource resource, InstrumentationScopeInfo scopeInfo) {
    LogRecordDataImpl.Builder logRecordData = LogRecordDataImpl.builder();

    logRecordData.setTimestampEpochNanos(source.getTimeUnixNano());
    logRecordData.setObservedTimestampEpochNanos(source.getObservedTimeUnixNano());
    logRecordData.setSeverity(severityNumberToSdk(source.getSeverityNumber()));
    logRecordData.setSeverityText(source.getSeverityText());
    if (source.hasBody()) {
      logRecordData.setBody(anyValueToBody(source.getBody()));
    }

    addExtrasToSdkItemBuilder(source, logRecordData, resource, scopeInfo);

    return logRecordData.build();
  }

  private static void addExtrasToSdkItemBuilder(
      LogRecord source,
      LogRecordDataImpl.Builder target,
      Resource resource,
      InstrumentationScopeInfo scopeInfo) {
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

  private static AnyValue bodyToAnyValue(Body body) {
    return AnyValue.newBuilder().setStringValue(body.asString()).build();
  }

  private static SeverityNumber severityToProto(Severity severity) {
    return SeverityNumber.forNumber(severity.getSeverityNumber());
  }

  private static Body anyValueToBody(AnyValue source) {
    if (source.hasStringValue()) {
      return Body.string(source.getStringValue());
    } else {
      return Body.empty();
    }
  }

  private static Severity severityNumberToSdk(SeverityNumber source) {
    for (Severity value : Severity.values()) {
      if (value.getSeverityNumber() == source.getNumber()) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
