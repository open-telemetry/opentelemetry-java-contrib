/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.squareup.wire.ProtoAdapter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.ProtoMetricsDataMapper;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public final class MetricDataSerializer implements SignalSerializer<MetricData> {
  private static final MetricDataSerializer INSTANCE = new MetricDataSerializer();

  private MetricDataSerializer() {}

  static MetricDataSerializer getInstance() {
    return INSTANCE;
  }

  @Override
  public byte[] serialize(Collection<MetricData> metricData) {
    MetricsData proto = ProtoMetricsDataMapper.getInstance().toProto(metricData);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      int size = MetricsData.ADAPTER.encodedSize(proto);
      ProtoAdapter.UINT32.encode(out, size);
      proto.encode(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public List<MetricData> deserialize(byte[] source) {
    try {
      return ProtoMetricsDataMapper.getInstance().fromProto(MetricsData.ADAPTER.decode(source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
