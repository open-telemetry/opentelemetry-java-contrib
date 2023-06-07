package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.data.EventDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.models.data.LinkDataImpl;
import io.opentelemetry.contrib.disk.buffer.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffer.testutils.TestData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SpanDataSerializerTest extends BaseSignalSerializerTest<SpanData> {

  private static final EventData EVENT_DATA =
      EventDataImpl.builder()
          .setAttributes(TestData.ATTRIBUTES)
          .setEpochNanos(1L)
          .setName("Event name")
          .setTotalAttributeCount(10)
          .build();

  private static final LinkData LINK_DATA =
      LinkDataImpl.builder()
          .setAttributes(TestData.ATTRIBUTES)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setTotalAttributeCount(20)
          .build();

  private static final LinkData LINK_DATA_WITH_TRACE_STATE =
      LinkDataImpl.builder()
          .setAttributes(TestData.ATTRIBUTES)
          .setSpanContext(TestData.SPAN_CONTEXT_WITH_TRACE_STATE)
          .setTotalAttributeCount(20)
          .build();

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

  @Test
  public void verifySerialization() {
    assertSerialization(SPAN_DATA);
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return SignalSerializer.ofSpans();
  }
}
