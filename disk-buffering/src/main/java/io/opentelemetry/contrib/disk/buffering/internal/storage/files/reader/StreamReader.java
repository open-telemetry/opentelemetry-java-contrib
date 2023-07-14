/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public abstract class StreamReader implements Closeable {
  protected final InputStream inputStream;

  protected StreamReader(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Nullable
  public abstract ReadResult read() throws IOException;

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public interface Factory {
    StreamReader create(InputStream stream);
  }
}
