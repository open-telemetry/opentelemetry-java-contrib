/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.ProtoSpansDataMapper;
import io.opentelemetry.proto.trace.v1.TracesData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public final class SpanDataSerializer implements SignalSerializer<SpanData> {
  @Nullable private static SpanDataSerializer instance;

  private SpanDataSerializer() {}

  static SpanDataSerializer get() {
    if (instance == null) {
      instance = new SpanDataSerializer();
    }
    return instance;
  }

  @Override
  public byte[] serialize(Collection<SpanData> spanData) {
    TracesData proto = ProtoSpansDataMapper.getInstance().toProto(spanData);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      proto.writeDelimitedTo(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<SpanData> deserialize(byte[] source) {
    try {
      return ProtoSpansDataMapper.getInstance().fromProto(TracesData.parseFrom(source));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
