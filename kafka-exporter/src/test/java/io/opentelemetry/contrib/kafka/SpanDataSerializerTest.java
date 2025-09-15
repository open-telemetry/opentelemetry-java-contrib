/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.contrib.kafka.TestUtil.makeBasicSpan;
import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(actual).isNotNull();
  }

  @Test
  void serializeEmptyData() {
    byte[] actual = testSubject.serialize("test-topic", Collections.emptySet());

    assertThat(actual).isEmpty();
  }

  @Test
  void convertSpansToRequest() {
    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-2");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    ExportTraceServiceRequest actual = testSubject.convertSpansToRequest(spans);

    assertThat(actual).isNotNull();
    assertThat(actual.getResourceSpans(0).getScopeSpans(0).getSpans(0).getName())
        .isEqualTo("span-1");
    assertThat(actual.getResourceSpans(0).getScopeSpans(0).getSpans(1).getName())
        .isEqualTo("span-2");
  }

  @Test
  void convertSpansToRequestForEmptySpans() {
    ExportTraceServiceRequest actual = testSubject.convertSpansToRequest(Collections.emptySet());

    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(ExportTraceServiceRequest.getDefaultInstance());
  }
}
