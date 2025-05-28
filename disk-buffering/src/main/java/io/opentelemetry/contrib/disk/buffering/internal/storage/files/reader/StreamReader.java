/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public interface StreamReader extends Closeable {
  @Nullable
  ReadResult readNext() throws IOException;

  interface Factory {
    StreamReader create(InputStream stream);
  }
}
