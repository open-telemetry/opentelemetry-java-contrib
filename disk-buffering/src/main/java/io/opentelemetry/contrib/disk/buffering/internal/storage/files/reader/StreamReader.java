/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileStream;
import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;

public interface StreamReader extends Closeable {
  @Nullable
  ReadResult read() throws IOException;

  interface Factory {
    StreamReader create(FileStream stream);
  }
}
