/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.ProtoMetricsDataMapper;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
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
  public byte[] serialize(Collection<MetricData> metricData) {
    MetricsData proto = ProtoMetricsDataMapper.INSTANCE.toProto(metricData);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      proto.writeDelimitedTo(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<MetricData> deserialize(byte[] source) {
    try {
      return ProtoMetricsDataMapper.INSTANCE.fromProto(MetricsData.parseFrom(source));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
