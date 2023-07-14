/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.List;

public interface SignalSerializer<SDK_ITEM> {

  static SignalSerializer<SpanData> ofSpans() {
    return SpanDataSerializer.getInstance();
  }

  static SignalSerializer<MetricData> ofMetrics() {
    return MetricDataSerializer.getInstance();
  }

  static SignalSerializer<LogRecordData> ofLogs() {
    return LogRecordDataSerializer.getInstance();
  }

  byte[] serialize(Collection<SDK_ITEM> items);

  List<SDK_ITEM> deserialize(byte[] source);
}
