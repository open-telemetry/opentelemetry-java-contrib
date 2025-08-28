/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpanDataDeserializerTest {
  private SpanDataDeserializer testSubject;

  @BeforeAll
  void setUp() {
    testSubject = new SpanDataDeserializer();
  }

  @Test
  void deserialize() {
    ResourceSpans resourceSpans =
        ResourceSpans.newBuilder().setResource(Resource.getDefaultInstance()).build();
    ExportTraceServiceRequest request =
        ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(resourceSpans)
            .addResourceSpans(resourceSpans)
            .build();
    byte[] data = request.toByteArray();

    ExportTraceServiceRequest actual = testSubject.deserialize("test-topic", data);

    assertThat(actual).isEqualTo(request);
  }

  @Test
  void deserializeNullData() {
    assertThat(testSubject.deserialize("test-topic", null)).isNull();
  }

  @Test
  void deserializeEmptyData() {
    assertThat(testSubject.deserialize("test-topic", new byte[0])).isNotNull();
  }
}
