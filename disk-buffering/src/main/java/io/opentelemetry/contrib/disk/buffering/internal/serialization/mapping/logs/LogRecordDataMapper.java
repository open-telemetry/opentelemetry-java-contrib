/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import static io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.SpanDataMapper.flagsFromInt;
import static io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools.toUnsignedInt;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
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

  private static final LogRecordDataMapper INSTANCE = new LogRecordDataMapper();

  public static LogRecordDataMapper getInstance() {
    return INSTANCE;
  }

  public LogRecord mapToProto(LogRecordData source) {
    LogRecord.Builder logRecord = new LogRecord.Builder();

    logRecord.time_unix_nano(source.getTimestampEpochNanos());
    logRecord.observed_time_unix_nano(source.getObservedTimestampEpochNanos());
    if (source.getSeverity() != null) {
      logRecord.severity_number(severityToProto(source.getSeverity()));
    }
    if (source.getSeverityText() != null) {
      logRecord.severity_text(source.getSeverityText());
    }
    if (source.getBody() != null) {
      logRecord.body(bodyToAnyValue(source.getBody()));
    }

    byte flags = source.getSpanContext().getTraceFlags().asByte();
    logRecord.flags(toUnsignedInt(flags));

    addExtrasToProtoBuilder(source, logRecord);

    return logRecord.build();
  }

  private static void addExtrasToProtoBuilder(LogRecordData source, LogRecord.Builder target) {
    target.attributes.addAll(
        AttributesMapper.getInstance().attributesToProto(source.getAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.span_id(ByteStringMapper.getInstance().stringToProto(spanContext.getSpanId()));
    target.trace_id(ByteStringMapper.getInstance().stringToProto(spanContext.getTraceId()));
    target.dropped_attributes_count(
        source.getTotalAttributeCount() - source.getAttributes().size());
  }

  public LogRecordData mapToSdk(
      LogRecord source, Resource resource, InstrumentationScopeInfo scopeInfo) {
    LogRecordDataImpl.Builder logRecordData = LogRecordDataImpl.builder();

    logRecordData.setTimestampEpochNanos(source.time_unix_nano);
    logRecordData.setObservedTimestampEpochNanos(source.observed_time_unix_nano);
    logRecordData.setSeverity(severityNumberToSdk(source.severity_number));
    logRecordData.setSeverityText(source.severity_text);
    if (source.body != null) {
      logRecordData.setBody(anyValueToBody(source.body));
    }

    addExtrasToSdkItemBuilder(source, logRecordData, resource, scopeInfo);

    return logRecordData.build();
  }

  private static void addExtrasToSdkItemBuilder(
      LogRecord source,
      LogRecordDataImpl.Builder target,
      Resource resource,
      InstrumentationScopeInfo scopeInfo) {
    Attributes attributes = AttributesMapper.getInstance().protoToAttributes(source.attributes);
    target.setAttributes(attributes);
    target.setSpanContext(
        SpanContext.create(
            ByteStringMapper.getInstance().protoToString(source.trace_id),
            ByteStringMapper.getInstance().protoToString(source.span_id),
            flagsFromInt(source.flags),
            TraceState.getDefault()));
    target.setTotalAttributeCount(source.dropped_attributes_count + attributes.size());
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scopeInfo);
  }

  private static AnyValue bodyToAnyValue(Body body) {
    return new AnyValue.Builder().string_value(body.asString()).build();
  }

  private static SeverityNumber severityToProto(Severity severity) {
    return SeverityNumber.fromValue(severity.getSeverityNumber());
  }

  private static Body anyValueToBody(AnyValue source) {
    if (source.string_value != null) {
      return Body.string(source.string_value);
    } else {
      return Body.empty();
    }
  }

  private static Severity severityNumberToSdk(SeverityNumber source) {
    for (Severity value : Severity.values()) {
      if (value.getSeverityNumber() == source.getValue()) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
