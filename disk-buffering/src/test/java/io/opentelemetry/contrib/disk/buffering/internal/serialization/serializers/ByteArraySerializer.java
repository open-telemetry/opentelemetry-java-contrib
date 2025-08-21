/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public final class ByteArraySerializer implements SignalSerializer<Object> {

  private final byte[] data;

  public ByteArraySerializer(byte[] data) {
    this.data = data;
  }

  @CanIgnoreReturnValue
  @Override
  public SignalSerializer<Object> initialize(Collection<Object> data) {
    return null;
  }

  @Override
  public void writeBinaryTo(OutputStream output) throws IOException {
    output.write(data);
  }

  @Override
  public int getBinarySerializedSize() {
    return data.length;
  }

  @Override
  public void reset() {}
}
