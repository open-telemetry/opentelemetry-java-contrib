/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SpanDataMapperTest {

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
  public void verifyMapping() {
    Span proto = mapToProto(SPAN_DATA);

    assertEquals(
        SPAN_DATA,
        mapToSdk(proto, SPAN_DATA.getResource(), SPAN_DATA.getInstrumentationScopeInfo()));
  }

  @Test
  public void verifyMappingWithTraceState() {
    Span proto = mapToProto(SPAN_DATA_WITH_TRACE_STATE);

    assertEquals(
        SPAN_DATA_WITH_TRACE_STATE,
        mapToSdk(
            proto,
            SPAN_DATA_WITH_TRACE_STATE.getResource(),
            SPAN_DATA_WITH_TRACE_STATE.getInstrumentationScopeInfo()));
  }

  private static Span mapToProto(SpanData source) {
    return SpanDataMapper.INSTANCE.mapToProto(source);
  }

  private static SpanData mapToSdk(
      Span source, Resource resource, InstrumentationScopeInfo instrumentationScopeInfo) {
    return SpanDataMapper.INSTANCE.mapToSdk(source, resource, instrumentationScopeInfo);
  }
}
