/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.TracesData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoSpansDataMapperTest {

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

  private static final SpanData OTHER_SPAN_DATA =
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

  private static final SpanData SPAN_DATA_WITH_DIFFERENT_SCOPE_SAME_RESOURCE =
      SpanDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
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

  private static final SpanData SPAN_DATA_WITH_DIFFERENT_RESOURCE =
      SpanDataImpl.builder()
          .setResource(TestData.RESOURCE_WITHOUT_SCHEMA_URL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
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

  @Test
  void verifyConversionDataStructure() {
    List<SpanData> signals = Collections.singletonList(SPAN_DATA);

    TracesData proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.getResourceSpansList();
    assertEquals(1, resourceSpans.size());
    assertEquals(1, resourceSpans.get(0).getScopeSpansList().size());
    assertEquals(1, resourceSpans.get(0).getScopeSpansList().get(0).getSpansList().size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithSameResourceAndScope() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, OTHER_SPAN_DATA);

    TracesData proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.getResourceSpansList();
    assertEquals(1, resourceSpans.size());
    List<ScopeSpans> scopeSpans = resourceSpans.get(0).getScopeSpansList();
    assertEquals(1, scopeSpans.size());
    List<Span> spans = scopeSpans.get(0).getSpansList();
    assertEquals(2, spans.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithSameResourceDifferentScope() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, SPAN_DATA_WITH_DIFFERENT_SCOPE_SAME_RESOURCE);

    TracesData proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.getResourceSpansList();
    assertEquals(1, resourceSpans.size());
    List<ScopeSpans> scopeSpans = resourceSpans.get(0).getScopeSpansList();
    assertEquals(2, scopeSpans.size());
    ScopeSpans firstScope = scopeSpans.get(0);
    ScopeSpans secondScope = scopeSpans.get(1);
    List<Span> firstScopeSpans = firstScope.getSpansList();
    List<Span> secondScopeSpans = secondScope.getSpansList();
    assertEquals(1, firstScopeSpans.size());
    assertEquals(1, secondScopeSpans.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithDifferentResource() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, SPAN_DATA_WITH_DIFFERENT_RESOURCE);

    TracesData proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.getResourceSpansList();
    assertEquals(2, resourceSpans.size());
    ResourceSpans firstResourceSpans = resourceSpans.get(0);
    ResourceSpans secondResourceSpans = resourceSpans.get(1);
    List<ScopeSpans> firstScopeSpans = firstResourceSpans.getScopeSpansList();
    List<ScopeSpans> secondScopeSpans = secondResourceSpans.getScopeSpansList();
    assertEquals(1, firstScopeSpans.size());
    assertEquals(1, secondScopeSpans.size());
    ScopeSpans firstScope = firstScopeSpans.get(0);
    ScopeSpans secondScope = secondScopeSpans.get(0);
    List<Span> firstSpans = firstScope.getSpansList();
    List<Span> secondSpans = secondScope.getSpansList();
    assertEquals(1, firstSpans.size());
    assertEquals(1, secondSpans.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  private static TracesData mapToProto(Collection<SpanData> signals) {
    return ProtoSpansDataMapper.getInstance().toProto(signals);
  }

  private static List<SpanData> mapFromProto(TracesData protoData) {
    return ProtoSpansDataMapper.getInstance().fromProto(protoData);
  }
}
