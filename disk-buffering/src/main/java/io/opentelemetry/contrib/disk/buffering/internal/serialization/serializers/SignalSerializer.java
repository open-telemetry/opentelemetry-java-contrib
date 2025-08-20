/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public interface SignalSerializer<SDK_ITEM> {

  static SignalSerializer<SpanData> ofSpans() {
    return new SpanDataSerializer();
  }

  static SignalSerializer<MetricData> ofMetrics() {
    return new MetricDataSerializer();
  }

  static SignalSerializer<LogRecordData> ofLogs() {
    return new LogRecordDataSerializer();
  }

  SignalSerializer<SDK_ITEM> initialize(Collection<SDK_ITEM> data);

  void writeBinaryTo(OutputStream output) throws IOException;

  int getBinarySerializedSize();

  void reset();
}
