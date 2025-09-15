/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
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

    ExportTraceServiceRequest proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.resource_spans;
    assertThat(resourceSpans).hasSize(1);
    assertThat(resourceSpans.get(0).scope_spans).hasSize(1);
    assertThat(resourceSpans.get(0).scope_spans.get(0).spans).hasSize(1);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithSameResourceAndScope() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, OTHER_SPAN_DATA);

    ExportTraceServiceRequest proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.resource_spans;
    assertThat(resourceSpans).hasSize(1);
    List<ScopeSpans> scopeSpans = resourceSpans.get(0).scope_spans;
    assertThat(scopeSpans).hasSize(1);
    List<Span> spans = scopeSpans.get(0).spans;
    assertThat(spans).hasSize(2);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithSameResourceDifferentScope() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, SPAN_DATA_WITH_DIFFERENT_SCOPE_SAME_RESOURCE);

    ExportTraceServiceRequest proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.resource_spans;
    assertThat(resourceSpans).hasSize(1);
    List<ScopeSpans> scopeSpans = resourceSpans.get(0).scope_spans;
    assertThat(scopeSpans).hasSize(2);
    ScopeSpans firstScope = scopeSpans.get(0);
    ScopeSpans secondScope = scopeSpans.get(1);
    List<Span> firstScopeSpans = firstScope.spans;
    List<Span> secondScopeSpans = secondScope.spans;
    assertThat(firstScopeSpans).hasSize(1);
    assertThat(secondScopeSpans).hasSize(1);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleSpansWithDifferentResource() {
    List<SpanData> signals = Arrays.asList(SPAN_DATA, SPAN_DATA_WITH_DIFFERENT_RESOURCE);

    ExportTraceServiceRequest proto = mapToProto(signals);

    List<ResourceSpans> resourceSpans = proto.resource_spans;
    assertThat(resourceSpans).hasSize(2);
    ResourceSpans firstResourceSpans = resourceSpans.get(0);
    ResourceSpans secondResourceSpans = resourceSpans.get(1);
    List<ScopeSpans> firstScopeSpans = firstResourceSpans.scope_spans;
    List<ScopeSpans> secondScopeSpans = secondResourceSpans.scope_spans;
    assertThat(firstScopeSpans).hasSize(1);
    assertThat(secondScopeSpans).hasSize(1);
    ScopeSpans firstScope = firstScopeSpans.get(0);
    ScopeSpans secondScope = secondScopeSpans.get(0);
    List<Span> firstSpans = firstScope.spans;
    List<Span> secondSpans = secondScope.spans;
    assertThat(firstSpans).hasSize(1);
    assertThat(secondSpans).hasSize(1);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  private static ExportTraceServiceRequest mapToProto(Collection<SpanData> signals) {
    return ProtoSpansDataMapper.getInstance().toProto(signals);
  }

  private static List<SpanData> mapFromProto(ExportTraceServiceRequest protoData) {
    return ProtoSpansDataMapper.getInstance().fromProto(protoData);
  }
}
