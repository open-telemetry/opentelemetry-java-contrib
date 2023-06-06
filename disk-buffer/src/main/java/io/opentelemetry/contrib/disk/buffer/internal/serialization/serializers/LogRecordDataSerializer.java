package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.logs.ResourceLogsDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.Serializer;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.logs.ResourceLogsData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.IOException;
import java.util.List;

public final class LogRecordDataSerializer implements SignalSerializer<LogRecordData> {
  private static LogRecordDataSerializer INSTANCE;

  private LogRecordDataSerializer() {}

  static LogRecordDataSerializer get() {
    if (INSTANCE == null) {
      INSTANCE = new LogRecordDataSerializer();
    }
    return INSTANCE;
  }

  @Override
  public byte[] serialize(List<LogRecordData> logRecordData) {
    try {
      return Serializer.serialize(ResourceLogsDataMapper.INSTANCE.toJsonDto(logRecordData));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<LogRecordData> deserialize(byte[] source) {
    try {
      ResourceLogsData deserialized = Serializer.deserialize(ResourceLogsData.class, source);
      return ResourceLogsDataMapper.INSTANCE.fromJsonDto(deserialized);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
