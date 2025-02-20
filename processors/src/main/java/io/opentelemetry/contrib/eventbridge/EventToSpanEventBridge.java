/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.eventbridge;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.internal.marshal.MarshalerWithSize;
import io.opentelemetry.exporter.internal.otlp.AnyValueMarshaler;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A processor that records events (i.e. log records with an {@code event.name} attribute) as span
 * events for the current span if:
 *
 * <ul>
 *   <li>The log record has a valid span context
 *   <li>{@link Span#current()} returns a span where {@link Span#isRecording()} is true
 *   <li>The log record's span context is the same as {@link Span#current()}
 * </ul>
 *
 * <p>The event {@link LogRecordData} is converted to a span event as follows:
 *
 * <ul>
 *   <li>{@code event.name} attribute is mapped to span event name
 *   <li>{@link LogRecordData#getTimestampEpochNanos()} is mapped to span event timestamp
 *   <li>{@link LogRecordData#getAttributes()} are mapped to span event attributes, excluding {@code
 *       event.name}
 *   <li>{@link LogRecordData#getObservedTimestampEpochNanos()} is mapped to span event attribute
 *       with key {@code log.record.observed_timestamp}
 *   <li>{@link LogRecordData#getSeverity()} is mapped to span event attribute with key {@code
 *       log.record.severity_number}
 *   <li>{@link LogRecordData#getBodyValue()} is mapped to span event attribute with key {@code
 *       log.record.body}, as an escaped JSON string following the standard protobuf JSON encoding
 *   <li>{@link LogRecordData#getTotalAttributeCount()} - {@link
 *       LogRecordData#getAttributes()}.size() is mapped to span event attribute with key {@code
 *       log.record.dropped_attributes_count}
 * </ul>
 */
public final class EventToSpanEventBridge implements LogRecordProcessor {

  private static final Logger logger = Logger.getLogger(EventToSpanEventBridge.class.getName());

  private static final AttributeKey<Long> LOG_RECORD_OBSERVED_TIME_UNIX_NANO =
      AttributeKey.longKey("log.record.observed_time_unix_nano");
  private static final AttributeKey<Long> LOG_RECORD_SEVERITY_NUMBER =
      AttributeKey.longKey("log.record.severity_number");
  private static final AttributeKey<String> LOG_RECORD_BODY =
      AttributeKey.stringKey("log.record.body");
  private static final AttributeKey<Long> LOG_RECORD_DROPPED_ATTRIBUTES_COUNT =
      AttributeKey.longKey("log.record.dropped_attributes_count");

  private EventToSpanEventBridge() {}

  /** Create an instance. */
  public static EventToSpanEventBridge create() {
    return new EventToSpanEventBridge();
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    LogRecordData logRecordData = logRecord.toLogRecordData();
    if (!(logRecordData instanceof ExtendedLogRecordData)) {
      return;
    }
    String eventName = ((ExtendedLogRecordData) logRecordData).getEventName();
    if (eventName == null) {
      return;
    }
    SpanContext logSpanContext = logRecordData.getSpanContext();
    if (!logSpanContext.isValid()) {
      return;
    }
    Span currentSpan = Span.current();
    if (!currentSpan.isRecording()) {
      return;
    }
    if (!currentSpan.getSpanContext().equals(logSpanContext)) {
      return;
    }
    currentSpan.addEvent(
        eventName,
        toSpanEventAttributes(logRecordData),
        logRecordData.getTimestampEpochNanos(),
        TimeUnit.NANOSECONDS);
  }

  private static Attributes toSpanEventAttributes(LogRecordData logRecord) {
    AttributesBuilder builder = logRecord.getAttributes().toBuilder();

    builder.put(LOG_RECORD_OBSERVED_TIME_UNIX_NANO, logRecord.getObservedTimestampEpochNanos());

    builder.put(LOG_RECORD_SEVERITY_NUMBER, logRecord.getSeverity().getSeverityNumber());

    // Add bridging for logRecord.getSeverityText() if EventBuilder adds severity text setter

    Value<?> body = logRecord.getBodyValue();
    if (body != null) {
      MarshalerWithSize marshaler = AnyValueMarshaler.create(body);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        marshaler.writeJsonTo(out);
        builder.put(LOG_RECORD_BODY, out.toString(StandardCharsets.UTF_8.name()));
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error converting log record body to JSON", e);
      }
    }

    int droppedAttributesCount =
        logRecord.getTotalAttributeCount() - logRecord.getAttributes().size();
    if (droppedAttributesCount > 0) {
      builder.put(LOG_RECORD_DROPPED_ATTRIBUTES_COUNT, droppedAttributesCount);
    }

    return builder.build();
  }

  @Override
  public String toString() {
    return "EventToSpanEventBridge{}";
  }
}
