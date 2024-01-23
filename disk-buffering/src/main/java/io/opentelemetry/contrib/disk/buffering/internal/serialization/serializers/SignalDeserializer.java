/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;

public interface SignalDeserializer<SDK_ITEM> {

  static SignalDeserializer<SpanData> ofSpans() {
    return SpanDataSerializer.getInstance();
  }

  static SignalDeserializer<MetricData> ofMetrics() {
    return MetricDataSerializer.getInstance();
  }

  static SignalDeserializer<LogRecordData> ofLogs() {
    return LogRecordDataSerializer.getInstance();
  }

  List<SDK_ITEM> deserialize(byte[] source);
}
