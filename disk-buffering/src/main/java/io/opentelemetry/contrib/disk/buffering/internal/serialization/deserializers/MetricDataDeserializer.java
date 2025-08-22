/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.ProtoMetricsDataMapper;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.util.List;

public final class MetricDataDeserializer implements SignalDeserializer<MetricData> {
  private static final MetricDataDeserializer INSTANCE = new MetricDataDeserializer();

  private MetricDataDeserializer() {}

  static MetricDataDeserializer getInstance() {
    return INSTANCE;
  }

  @Override
  public List<MetricData> deserialize(byte[] source) throws DeserializationException {
    try {
      return ProtoMetricsDataMapper.getInstance()
          .fromProto(ExportMetricsServiceRequest.ADAPTER.decode(source));
    } catch (IOException e) {
      throw new DeserializationException(e);
    }
  }
}
