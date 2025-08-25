/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import io.opentelemetry.exporter.internal.otlp.metrics.LowAllocationMetricsRequestMarshaler;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class MetricDataSerializer implements SignalSerializer<MetricData> {

  private final LowAllocationMetricsRequestMarshaler marshaler =
      new LowAllocationMetricsRequestMarshaler();

  MetricDataSerializer() {}

  @CanIgnoreReturnValue
  @Override
  public MetricDataSerializer initialize(Collection<MetricData> data) {
    marshaler.initialize(data);
    return this;
  }

  @Override
  public void writeBinaryTo(OutputStream output) throws IOException {
    ProtobufTools.writeRawVarint32(marshaler.getBinarySerializedSize(), output);
    marshaler.writeBinaryTo(output);
  }

  @Override
  public int getBinarySerializedSize() {
    return marshaler.getBinarySerializedSize();
  }

  @Override
  public void reset() {
    marshaler.reset();
  }
}
