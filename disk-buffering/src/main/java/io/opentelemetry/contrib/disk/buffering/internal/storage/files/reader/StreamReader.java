/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/**
 * Reads items one at a time from an input stream.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface StreamReader extends Closeable {
  @Nullable
  byte[] readNext() throws IOException;

  /**
   * Creates {@link StreamReader} instances.
   *
   * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
   * at any time.
   */
  interface Factory {
    StreamReader create(InputStream stream);
  }
}
