/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.ProtoSpansDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.proto.trace.v1.TracesData;
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
  public List<SpanData> deserialize(byte[] source) {
    try {
      return ProtoSpansDataMapper.getInstance().fromProto(TracesData.ADAPTER.decode(source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String signalType() {
    return SignalTypes.spans.name();
  }
}
