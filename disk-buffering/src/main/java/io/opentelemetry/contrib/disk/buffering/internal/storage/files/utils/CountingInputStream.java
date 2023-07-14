/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class CountingInputStream extends FilterInputStream {

  private int position;
  private int mark = -1;

  public CountingInputStream(InputStream in) {
    super(in);
  }

  public int getPosition() {
    return position;
  }

  @Override
  public synchronized void mark(int readlimit) {
    in.mark(readlimit);
    mark = position;
  }

  @Override
  public long skip(long n) throws IOException {
    long result = in.skip(n);
    position = (int) (position + result);
    return result;
  }

  @Override
  public int read() throws IOException {
    int result = in.read();
    if (result != -1) {
      position++;
    }
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int result = in.read(b, off, len);
    if (result != -1) {
      position += result;
    }
    return result;
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark is not supported");
    }
    if (mark == -1) {
      throw new IOException("Mark is not set");
    }

    in.reset();
    position = mark;
  }
}
