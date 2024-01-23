/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.ProtoMetricsDataMapper;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.util.List;

public final class MetricDataDeerializer implements SignalDeserializer<MetricData> {
  private static final MetricDataDeerializer INSTANCE = new MetricDataDeerializer();

  private MetricDataDeerializer() {}

  static MetricDataDeerializer getInstance() {
    return INSTANCE;
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
