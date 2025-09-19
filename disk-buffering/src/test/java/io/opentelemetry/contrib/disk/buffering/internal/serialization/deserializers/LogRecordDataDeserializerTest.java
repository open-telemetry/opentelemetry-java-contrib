/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import org.junit.jupiter.api.Test;

class LogRecordDataDeserializerTest extends BaseSignalSerializerTest<LogRecordData> {
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
          .setEventName("event")
          .build();

  @Test
  void verifyDeserialization() {
    assertSerializeDeserialize(LOG_RECORD, LOG_RECORD);
  }

  @Test
  void whenDecodingMalformedMessage_wrapIntoDeserializationException() {
    assertThatThrownBy(() -> getDeserializer().deserialize(TestData.makeMalformedSignalBinary()))
        .isInstanceOf(DeserializationException.class);
  }

  @Test
  void whenDecodingTooShortMessage_wrapIntoDeserializationException() {
    assertThatThrownBy(() -> getDeserializer().deserialize(TestData.makeTooShortSignalBinary()))
        .isInstanceOf(DeserializationException.class);
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
