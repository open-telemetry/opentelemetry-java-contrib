/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
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
          .build();

  @Test
  void verifyMapping() {
    LogRecord proto = mapToProto(LOG_RECORD);

    assertEquals(
        LOG_RECORD,
        mapToSdk(proto, LOG_RECORD.getResource(), LOG_RECORD.getInstrumentationScopeInfo()));
  }

  private static LogRecord mapToProto(LogRecordData data) {
    return LogRecordDataMapper.getInstance().mapToProto(data);
  }

  private static LogRecordData mapToSdk(
      LogRecord data, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return LogRecordDataMapper.getInstance().mapToSdk(data, resource, scopeInfo);
  }
}
