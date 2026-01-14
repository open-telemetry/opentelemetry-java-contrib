/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import io.opentelemetry.exporter.internal.otlp.traces.LowAllocationTraceRequestMarshaler;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public final class SpanDataSerializer implements Serializer<Collection<SpanData>> {
  @Override
  public byte[] serialize(String topic, Collection<SpanData> data) {
    if (Objects.isNull(data)) {
      throw new SerializationException("Cannot serialize null");
    }
    return convertSpansToRequest(data).toByteArray();
  }

  ExportTraceServiceRequest convertSpansToRequest(Collection<SpanData> spans) {
    // Use LowAllocationTraceRequestMarshaler for more efficient conversion
    // This eliminates unnecessary serialization/deserialization cycles
    LowAllocationTraceRequestMarshaler marshaler = new LowAllocationTraceRequestMarshaler();
    try {
      marshaler.initialize(spans);

      // Serialize to bytes and parse back to protobuf message
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      marshaler.writeBinaryTo(baos);
      return ExportTraceServiceRequest.parseFrom(baos.toByteArray());
    } catch (IOException e) {
      throw new SerializationException(e);
    } finally {
      marshaler.reset();
    }
  }
}
