/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import org.junit.jupiter.api.Test;

class LogRecordDataSerializerTest extends BaseSignalSerializerTest<LogRecordData> {
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

  private static final LogRecordData LOG_RECORD_WITHOUT_SEVERITY_TEXT =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(Attributes.empty())
          .setBodyValue(Value.of("Log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  @Test
  void verifySerialization() {
    assertSerialization(LOG_RECORD, LOG_RECORD_WITHOUT_SEVERITY_TEXT);
  }

  @Override
  protected SignalSerializer<LogRecordData> getSerializer() {
    return SignalSerializer.ofLogs();
  }

  @Override
  protected SignalDeserializer<LogRecordData> getDeserializer() {
    return SignalDeserializer.ofLogs();
  }
}
