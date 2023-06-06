package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans.ResourceSpansDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.JsonSerializer;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans.ResourceSpansData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
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
  public byte[] serialize(List<SpanData> spanData) {
    try {
      return JsonSerializer.serialize(ResourceSpansDataMapper.INSTANCE.toJsonDto(spanData));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<SpanData> deserialize(byte[] source) {
    try {
      return ResourceSpansDataMapper.INSTANCE.fromJsonDto(
          JsonSerializer.deserialize(ResourceSpansData.class, source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
