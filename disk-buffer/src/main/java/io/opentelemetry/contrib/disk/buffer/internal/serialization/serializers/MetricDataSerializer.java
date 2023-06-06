package io.opentelemetry.contrib.disk.buffer.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.ResourceMetricsDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.JsonSerializer;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.ResourceMetricsData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

public final class MetricDataSerializer implements SignalSerializer<MetricData> {
  @Nullable private static MetricDataSerializer instance;

  private MetricDataSerializer() {}

  static MetricDataSerializer get() {
    if (instance == null) {
      instance = new MetricDataSerializer();
    }
    return instance;
  }

  @Override
  public byte[] serialize(List<MetricData> metricData) {
    try {
      return JsonSerializer.serialize(ResourceMetricsDataMapper.INSTANCE.toJsonDto(metricData));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<MetricData> deserialize(byte[] source) {
    try {
      return ResourceMetricsDataMapper.INSTANCE.fromJsonDto(
          JsonSerializer.deserialize(ResourceMetricsData.class, source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
