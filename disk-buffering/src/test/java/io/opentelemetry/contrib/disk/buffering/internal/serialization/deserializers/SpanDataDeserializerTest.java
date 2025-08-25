/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SpanDataDeserializerTest extends BaseSignalSerializerTest<SpanData> {
  private static final SpanData SPAN_DATA =
      SpanDataImpl.builder()
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setParentSpanContext(TestData.PARENT_SPAN_CONTEXT)
          .setName("Test span")
          .setKind(SpanKind.SERVER)
          .setStartEpochNanos(100L)
          .setEndEpochNanos(200L)
          .setStatus(StatusData.ok())
          .setAttributes(TestData.ATTRIBUTES)
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setTotalRecordedLinks(0)
          .setTotalRecordedEvents(0)
          .setTotalAttributeCount(0)
          .setEvents(Collections.emptyList())
          .setLinks(Collections.emptyList())
          .build();

  @Test
  void verifyDeserialization() {
    assertSerializeDeserialize(SPAN_DATA, SPAN_DATA);
  }

  @Test
  void whenDecodingMalformedMessage_wrapIntoDeserializationException() {
    assertThrows(
        DeserializationException.class,
        () -> getDeserializer().deserialize(TestData.makeMalformedSignalBinary()));
  }

  @Test
  void whenDecodingTooShortMessage_wrapIntoDeserializationException() {
    assertThrows(
        DeserializationException.class,
        () -> getDeserializer().deserialize(TestData.makeTooShortSignalBinary()));
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return SignalSerializer.ofSpans();
  }

  @Override
  protected SignalDeserializer<SpanData> getDeserializer() {
    return SignalDeserializer.ofSpans();
  }
}
