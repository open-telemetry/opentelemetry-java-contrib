/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SpanDataSerializerTest extends BaseSignalSerializerTest<SpanData> {

  private static final EventData EVENT_DATA =
      EventData.create(1L, "Event name", TestData.ATTRIBUTES, 10);

  private static final LinkData LINK_DATA =
      LinkData.create(TestData.SPAN_CONTEXT, TestData.ATTRIBUTES, 20);

  private static final LinkData LINK_DATA_WITH_TRACE_STATE =
      LinkData.create(TestData.SPAN_CONTEXT_WITH_TRACE_STATE, TestData.ATTRIBUTES, 20);

  private static final SpanData SPAN_DATA =
      SpanDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Span name")
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setParentSpanContext(TestData.PARENT_SPAN_CONTEXT)
          .setAttributes(TestData.ATTRIBUTES)
          .setStartEpochNanos(1L)
          .setEndEpochNanos(2L)
          .setKind(SpanKind.CLIENT)
          .setStatus(StatusData.error())
          .setEvents(Collections.singletonList(EVENT_DATA))
          .setLinks(Arrays.asList(LINK_DATA, LINK_DATA_WITH_TRACE_STATE))
          .setTotalAttributeCount(10)
          .setTotalRecordedEvents(2)
          .setTotalRecordedLinks(2)
          .build();

  private static final SpanData SPAN_DATA_WITH_TRACE_STATE =
      SpanDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Span name2")
          .setSpanContext(TestData.SPAN_CONTEXT_WITH_TRACE_STATE)
          .setParentSpanContext(TestData.PARENT_SPAN_CONTEXT)
          .setAttributes(TestData.ATTRIBUTES)
          .setStartEpochNanos(1L)
          .setEndEpochNanos(2L)
          .setKind(SpanKind.CLIENT)
          .setStatus(StatusData.error())
          .setEvents(Collections.singletonList(EVENT_DATA))
          .setLinks(Collections.singletonList(LINK_DATA))
          .setTotalAttributeCount(10)
          .setTotalRecordedEvents(2)
          .setTotalRecordedLinks(2)
          .build();

  @Test
  public void verifySerialization() {
    assertSerialization(SPAN_DATA, SPAN_DATA_WITH_TRACE_STATE);
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return SignalSerializer.ofSpans();
  }
}
