/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

class NoopSerializer<T> implements SignalSerializer<T> {

  @Override
  public NoopSerializer<T> initialize(Collection<T> data) {
    return this;
  }

  @Override
  public void writeBinaryTo(OutputStream output) throws IOException {}

  @Override
  public int getBinarySerializedSize() {
    return 0;
  }

  @Override
  public void reset() {}
}
