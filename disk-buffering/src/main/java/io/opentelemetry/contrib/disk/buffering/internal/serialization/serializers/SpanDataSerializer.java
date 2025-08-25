/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import io.opentelemetry.exporter.internal.otlp.traces.LowAllocationTraceRequestMarshaler;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class SpanDataSerializer implements SignalSerializer<SpanData> {

  private final LowAllocationTraceRequestMarshaler marshaler =
      new LowAllocationTraceRequestMarshaler();

  SpanDataSerializer() {}

  @CanIgnoreReturnValue
  @Override
  public SpanDataSerializer initialize(Collection<SpanData> data) {
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
