/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.contrib.kafka.TestUtil.makeBasicSpan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.ImmutableList;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpanDataSerializerTest {
  private final SpanDataSerializer testSubject = new SpanDataSerializer();

  @Test
  void serialize() {
    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-1");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    byte[] actual = testSubject.serialize("test-topic", spans);

    assertNotNull(actual);
  }

  @Test
  void serializeEmptyData() {
    byte[] actual = testSubject.serialize("test-topic", Collections.emptySet());

    assertEquals(0, actual.length);
  }

  @Test
  void convertSpansToRequest() {
    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-2");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    ExportTraceServiceRequest actual = testSubject.convertSpansToRequest(spans);

    assertNotNull(actual);
    assertEquals("span-1", actual.getResourceSpans(0).getScopeSpans(0).getSpans(0).getName());
    assertEquals("span-2", actual.getResourceSpans(0).getScopeSpans(0).getSpans(1).getName());
  }

  @Test
  void convertSpansToRequestForEmptySpans() {
    ExportTraceServiceRequest actual = testSubject.convertSpansToRequest(Collections.emptySet());

    assertNotNull(actual);
    assertEquals(ExportTraceServiceRequest.getDefaultInstance(), actual);
  }
}
