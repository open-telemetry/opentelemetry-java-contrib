/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.ProtoSpansDataMapper;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.List;

public final class SpanDataDeserializer implements SignalDeserializer<SpanData> {
  private static final SpanDataDeserializer INSTANCE = new SpanDataDeserializer();

  private SpanDataDeserializer() {}

  static SpanDataDeserializer getInstance() {
    return INSTANCE;
  }

  @Override
  public List<SpanData> deserialize(byte[] source) throws DeserializationException {
    try {
      return ProtoSpansDataMapper.getInstance()
          .fromProto(ExportTraceServiceRequest.ADAPTER.decode(source));
    } catch (IOException | IllegalStateException e) {
      throw new DeserializationException(e);
    }
  }
}
