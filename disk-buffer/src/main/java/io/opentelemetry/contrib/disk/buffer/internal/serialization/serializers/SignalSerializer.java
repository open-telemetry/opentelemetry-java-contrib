package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import java.util.List;

public interface SignalSerializer<SDK_ITEM> {

  static SpanDataSerializer ofSpans() {
    return SpanDataSerializer.get();
  }

  static MetricDataSerializer ofMetrics() {
    return MetricDataSerializer.get();
  }

  static LogRecordDataSerializer ofLogs() {
    return LogRecordDataSerializer.get();
  }

  byte[] serialize(List<SDK_ITEM> items);

  List<SDK_ITEM> deserialize(byte[] source);
}
