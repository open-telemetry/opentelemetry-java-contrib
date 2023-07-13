/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import java.util.Collection;
import java.util.List;

public interface SignalSerializer<SDK_ITEM> {

  static SpanDataSerializer ofSpans() {
    return SpanDataSerializer.get();
  }

  static MetricDataSerializer ofMetrics() {
    return MetricDataSerializer.getInstance();
  }

  static LogRecordDataSerializer ofLogs() {
    return LogRecordDataSerializer.getInstance();
  }

  byte[] serialize(Collection<SDK_ITEM> items);

  List<SDK_ITEM> deserialize(byte[] source);
}
