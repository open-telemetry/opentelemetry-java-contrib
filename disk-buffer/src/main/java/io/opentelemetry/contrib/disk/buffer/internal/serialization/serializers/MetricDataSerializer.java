package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.List;

public final class MetricDataSerializer implements SignalSerializer<MetricData> {
  private static MetricDataSerializer INSTANCE;

  private MetricDataSerializer() {}

  static MetricDataSerializer get() {
    if (INSTANCE == null) {
      INSTANCE = new MetricDataSerializer();
    }
    return INSTANCE;
  }

  @Override
  public byte[] serialize(List<MetricData> metricData) {
    return new byte[0];
  }

  @Override
  public List<MetricData> deserialize(byte[] source) {
    return null;
  }
}
