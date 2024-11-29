/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;

public interface SignalDeserializer<SDK_ITEM> {

  static SignalDeserializer<SpanData> ofSpans() {
    return SpanDataDeserializer.getInstance();
  }

  static SignalDeserializer<MetricData> ofMetrics() {
    return MetricDataDeserializer.getInstance();
  }

  static SignalDeserializer<LogRecordData> ofLogs() {
    return LogRecordDataDeserializer.getInstance();
  }

  /** Deserializes the given byte array into a list of telemetry items. */
  List<SDK_ITEM> deserialize(byte[] source) throws DeserializationException;

  /** Returns the name of the stored type of signal -- one of "metrics", "spans", or "logs". */
  default String signalType() {
    return "unknown";
  }
}
