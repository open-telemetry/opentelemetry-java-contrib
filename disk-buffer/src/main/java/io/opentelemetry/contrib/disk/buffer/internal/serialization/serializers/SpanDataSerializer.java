package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;

public final class SpanDataSerializer implements SignalSerializer<SpanData> {
  private static SpanDataSerializer INSTANCE;

  private SpanDataSerializer() {}

  static SpanDataSerializer get() {
    if (INSTANCE == null) {
      INSTANCE = new SpanDataSerializer();
    }
    return INSTANCE;
  }

  @Override
  public byte[] serialize(List<SpanData> spanData) {
    return new byte[0];
  }

  @Override
  public List<SpanData> deserialize(byte[] source) {
    return null;
  }
}
