/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    assertEquals(request, actual);
  }

  @Test
  void deserializeNullData() {
    assertNotNull(testSubject.deserialize("test-topic", null));
  }

  @Test
  void deserializeEmptyData() {
    assertNotNull(testSubject.deserialize("test-topic", new byte[0]));
  }
}
