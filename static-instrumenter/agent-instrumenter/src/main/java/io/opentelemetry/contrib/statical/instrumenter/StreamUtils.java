/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.statical.instrumenter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class StreamUtils {

  private StreamUtils() {}

  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[4 * 1024];
    int read = in.read(buf);
    while (read != -1) {
      out.write(buf, 0, read);
      read = in.read(buf);
    }
  }
}
