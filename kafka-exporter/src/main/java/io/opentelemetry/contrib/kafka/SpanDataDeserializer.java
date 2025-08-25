/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.Objects;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public final class SpanDataDeserializer implements Deserializer<ExportTraceServiceRequest> {
  @SuppressWarnings("NullAway")
  @Override
  public ExportTraceServiceRequest deserialize(String topic, byte[] data) {
    if (Objects.isNull(data)) {
      return null;
    }
    try {
      return ExportTraceServiceRequest.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new SerializationException("Error while deserializing data", e);
    }
  }
}
