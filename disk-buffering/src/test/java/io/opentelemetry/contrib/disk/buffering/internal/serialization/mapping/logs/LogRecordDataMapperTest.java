/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class LogRecordDataMapperTest {

  private static final LogRecordData LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("my.event.name")
          .build();

  private static final LogRecordData LOG_RECORD_WITH_EMPTY_STRING_BODY =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of(""))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("my.event.name")
          .build();

  @Test
  void verifyMapping() throws IOException {
    LogRecord proto = encodeAndDecode(mapToProto(LOG_RECORD));

    assertThat(mapToSdk(proto, LOG_RECORD.getResource(), LOG_RECORD.getInstrumentationScopeInfo()))
        .isEqualTo(LOG_RECORD);
  }

  @Test
  void verifyEmptyBodyMapping() throws IOException {
    LogRecord proto = encodeAndDecode(mapToProto(LOG_RECORD_WITH_EMPTY_STRING_BODY));

    LogRecord emptyBodyProto = proto.newBuilder()
        // It can't be replicated in tests, but in production, after decoding the protobuf,
        // we ended up with an empty AnyValue when the original body was an empty string.
        // So we are faking it.
        .body(new AnyValue.Builder().build())
        .build();

    assertThat(
        mapToSdk(
            emptyBodyProto,
            LOG_RECORD_WITH_EMPTY_STRING_BODY.getResource(),
            LOG_RECORD_WITH_EMPTY_STRING_BODY.getInstrumentationScopeInfo()
        )
    ).isEqualTo(LOG_RECORD_WITH_EMPTY_STRING_BODY);
  }

  private static LogRecord mapToProto(LogRecordData data) {
    return LogRecordDataMapper.getInstance().mapToProto(data);
  }

  private static LogRecordData mapToSdk(
      LogRecord data, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return LogRecordDataMapper.getInstance().mapToSdk(data, resource, scopeInfo);
  }

  private static LogRecord encodeAndDecode(LogRecord proto) throws IOException {
    return LogRecord.ADAPTER.decode(proto.encode());
  }
}
